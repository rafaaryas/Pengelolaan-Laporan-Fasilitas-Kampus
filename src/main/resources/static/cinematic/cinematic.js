import * as T from './vendor/three.module.min.js';
import { GLTFLoader } from './vendor/GLTFLoader.js';
import { bindRig,poseRig } from './rig.js';
import { makeEarth,makeSpace,lightStudio } from './space.js';
import { makeSequence,DURATION } from './sequence.js';

const mix=T.MathUtils.lerp;
export async function createCinematic(canvas, {
  onFailure = () => {}, onScrim = () => {}, startAtEnd = false, paused = false, signal
} = {}) {
  signal?.throwIfAborted();
  const compact = matchMedia('(max-width: 768px)').matches;
  const renderer=new T.WebGLRenderer({canvas,alpha:false,antialias:!compact,powerPreference:'low-power'});
  renderer.shadowMap.enabled=true;renderer.shadowMap.type=T.PCFSoftShadowMap;renderer.setClearColor(0,0);renderer.toneMapping=T.ACESFilmicToneMapping;
  renderer.toneMappingExposure=1.0;
  const scene=new T.Scene(),camera=new T.PerspectiveCamera(36,1,.1,260);
  const {timeline,pose}=makeSequence();
  let mascot,mixer,clip,idleApplied=false,rest=[],shoulderMounts=[];
  const sky=makeSpace(scene),studio=lightStudio(scene,renderer);
  const key=scene.getObjectByName('StudioKey');
  const state={time:startAtEnd?DURATION:0,playing:!paused,elapsedIdle:0};
  let earth,frame=0,last=0,disposed=false,quality=1,slow=0,samples=0;
  let observer,lastIdlePaint=0;
  const lowQuality = .65;
  function fitShadow(width) {
    const size = width <= 768 ? 768 : 1024;
    if (key.shadow.mapSize.width === size) return;
    key.shadow.map?.dispose();
    key.shadow.map = null;
    key.shadow.mapSize.set(size, size);
  }
  function resize() {
    const box=canvas.getBoundingClientRect();
    const w=Math.max(1,box.width),h=Math.max(1,box.height);
    fitShadow(w);
    const ratio=Math.min(window.devicePixelRatio||1,w<h?1.35:1.4,Math.sqrt(2160000/(w*h)))*quality;
    renderer.setPixelRatio(ratio);renderer.setSize(Math.round(w),Math.round(h),false);
    camera.aspect=w/h;sky.uniforms.dpr.value=ratio;
    if (!document.hidden) draw();
  }
  function draw(){
    if(disposed||!earth)return;
    const t=state.time, idle=state.elapsedIdle, a=camera.aspect;
    timeline.time(t,true);
    const wide=T.MathUtils.smoothstep(a,.77,1.48),p=pose.push;
    const openFov=mix(43,35,wide), finalFov=mix(39,30,wide);
    const openZ=Math.max(31.7,5/Math.sin(Math.atan(Math.tan(openFov*Math.PI/360)*a*.69)));
    const finalZ=mix(4.7,5.65,wide),finalY=mix(5.30,5.50,wide);
    camera.fov=mix(openFov,finalFov,p);
    camera.position.set(mix(-.29,0,p),mix(.02,finalY,p),mix(openZ,finalZ,p));
    camera.rotation.set(0,mix(-.008,0,p),mix(-.018,0,p));camera.updateProjectionMatrix();
    const z=-54, finalDepth=finalZ-z,half=finalDepth*Math.tan(finalFov*Math.PI/360);
    // Foreshorten the portrait reach so the hands frame the mask in narrow viewports.
    const portrait=1-T.MathUtils.smoothstep(a,.56,1.34);
    const scale=Math.min(half*mix(.425,.372,wide),half*a*.94/mix(2.30,1.96,portrait));
    shoulderMounts.forEach((mount,i)=>{mount.rotation.y=(i?-1:1)*portrait*.61;});
    // Set character height independently of the foreground horizon so Earth can conceal the legs.
    const framingY=mix(5.30,5.84,wide);
    const framingAngle=Math.asin(5/Math.hypot(framingY,finalZ))-Math.atan2(framingY,finalZ);
    const characterBaseline=finalY+finalDepth*Math.tan(framingAngle);
    const breath=Math.sin(idle*1.75);
    // Preserve the rise from behind Earth while lifting the face above the foreground copy.
    const y=characterBaseline+scale*mix(.80,.63,wide)+half*mix(.185,.355,wide);
    mascot.root.scale.setScalar(scale);
    mascot.root.position.set(0,
      y-(1-pose.lift)*scale*mix(3.18,3.80,wide)-(1-p)*52+breath*.014*scale,z);
    key.position.copy(mascot.root.position);key.position.x-=3.9*scale;key.position.y+=4.5*scale;key.position.z+=mix(4.1,6.4,portrait)*scale;
    const cam=key.shadow.camera;cam.left=cam.bottom=-scale*2.6;cam.right=cam.top=scale*2.6;cam.near=scale*2;cam.far=scale*12;
    cam.updateProjectionMatrix();key.shadow.normalBias=.007*scale;
    mascot.root.rotation.set(0,0,pose.lean*.65);
    if(idleApplied){
      for(const {node,q,scale:restScale} of rest){node.quaternion.copy(q);node.scale.copy(restScale);}
      idleApplied=false;
    }
    clip.enabled=true;clip.paused=false;mixer.setTime(t);
    if(t===DURATION && idle>0){
      for(const part of rest){part.q.copy(part.node.quaternion);part.scale.copy(part.node.scale);}
      poseRig(mascot,pose,idle);idleApplied=true;
    }
    earth.globe.rotation.set(mix(-.26,-.79,p),0,-.15);
    earth.rotate(t+idle*.13);sky.uniforms.clock.value=t+idle;sky.uniforms.idleGlow.value=Math.min(1,idle);
    // The darker mask follows the reveal. Give the opening planet more room to read.
    onScrim(mix(.66,1,T.MathUtils.smoothstep(t,1.30,2.38)));
    renderer.render(scene,camera);
  }
  function stopClock(){cancelAnimationFrame(frame);frame=0;last=0;lastIdlePaint=0;}
  function queue(){
    if(!frame&&!disposed&&!document.hidden&&state.playing)frame=requestAnimationFrame(step);
  }
  function step(now){
    frame=0;
    if(disposed||document.hidden||!state.playing)return;
    const dt=last?Math.max(0,(now-last)/1000):0;last=now;
    if(dt>0){
      samples++; if(dt>.041)slow++;
      if(samples===36){
        if(slow>23){
          if(quality<=lowQuality){lost();return;}
          quality=Math.max(lowQuality,quality*.82);resize();
        }
        samples=slow=0;
      }
    }
    if(state.time<DURATION)state.time=Math.min(DURATION,state.time+dt);
    else {
      state.elapsedIdle+=dt;
      if(now-lastIdlePaint<1000/30-1){queue();return;}
      lastIdlePaint=now;
    }
    draw();queue();
  }
  function play(){
    state.playing=true;last=0;draw();queue();
  }
  function pause(){stopClock();state.playing=false;draw();}
  function visibility(){
    if(document.hidden)stopClock();
    else if(state.playing)queue();
    else draw();
  }
  function lost(event){event?.preventDefault();dispose();onFailure();}
  function dispose(){
    if(disposed)return;disposed=true;stopClock();timeline.kill();observer?.disconnect();
    document.removeEventListener('visibilitychange',visibility);canvas.removeEventListener('webglcontextlost',lost);
    const items=new Set();scene.traverse(o=>{
      if(o.geometry)items.add(o.geometry);
      if(o.skeleton)items.add(o.skeleton);
      if(o.material){items.add(o.material);if(o.material.map)items.add(o.material.map);}
    });
    for(const o of [...items,...(earth?.textures||[])])o.dispose();
    if(mixer){mixer.stopAllAction();mixer.uncacheRoot(mascot.root);}
    key.shadow.dispose();studio.dispose();renderer.dispose();
    if(!renderer.getContext().isContextLost())renderer.forceContextLoss();
  }
  try{
    const [model,planet]=await Promise.allSettled([
      new GLTFLoader().loadAsync(new URL('./assets/cakra.glb',import.meta.url).href),
      makeEarth(scene,renderer.capabilities.getMaxAnisotropy(),compact || renderer.capabilities.maxTextureSize < 4096)
    ]);
    if(planet.status==='fulfilled')earth=planet.value;
    if(model.status==='rejected')throw model.reason;
    const asset=model.value;
    mascot=bindRig(asset.scene.getObjectByName('Cakra'));scene.add(mascot.root);
    mascot.root.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true;}});
    key.target=mascot.root;
    shoulderMounts=mascot.arms.map(({arm})=>{
      const mount=new T.Group();mount.position.copy(arm.position);
      arm.parent.add(mount);mount.add(arm);arm.position.set(0,0,0);return mount;
    });
    rest=[mascot.body,mascot.head,...mascot.eyes,...mascot.glints,...mascot.ribbons,...mascot.arms.flatMap(a=>[a.arm,a.elbow,a.palm])]
      .map(node=>({node,q:new T.Quaternion(),scale:new T.Vector3()}));
    mixer=new T.AnimationMixer(mascot.root);clip=mixer.clipAction(asset.animations[0]);
    clip.setLoop(T.LoopOnce,1);clip.clampWhenFinished=true;clip.play();
    if(planet.status==='rejected')throw planet.reason;
    signal?.throwIfAborted();
    // Warm both the lit character and shadow pass before starting the 4.5-second clock.
    if(renderer.extensions.has('KHR_parallel_shader_compile'))await renderer.compileAsync(scene,camera);
    else renderer.compile(scene,camera);
    const opening=state.time;state.time=DURATION;resize();
    signal?.throwIfAborted();
    state.time=opening;
    if (!document.hidden) draw();
    observer=new ResizeObserver(resize);observer.observe(canvas);
    document.addEventListener('visibilitychange',visibility);canvas.addEventListener('webglcontextlost',lost);
    queue();
  }catch(e){dispose();throw e;}
  return {play,pause,dispose};
}
