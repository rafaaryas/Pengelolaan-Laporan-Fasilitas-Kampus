import * as T from './vendor/three.module.min.js';

const globeVertex = `
varying vec2 coords;
varying vec3 N;
varying vec3 world;
void main(){
 coords=uv;
 world=(modelMatrix*vec4(position,1.)).xyz;
 N=normalize(mat3(modelMatrix)*normal);
 gl_Position=projectionMatrix*viewMatrix*vec4(world,1.);
}`;
const sun=new T.Vector3(-.92,.27,.24).normalize();
const texURL = name => new URL('./assets/'+name+'.webp',import.meta.url).href;
export async function makeEarth(scene, limit, compact) {
  const globe=new T.Group();scene.add(globe);
  const loader = new T.TextureLoader();
  const suffix = compact ? '-compact' : '';
  const maps = await Promise.allSettled(['earth', 'clouds', 'night'].map(name => loader.loadAsync(texURL(name + suffix))));
  if(maps.some(m=>m.status==='rejected')){
    for(const m of maps) if(m.status==='fulfilled')m.value.dispose();
    throw Error('Tekstur bumi gagal dimuat');
  }
  const [day,cloud,night]=maps.map(m=>m.value);
  for(const tex of [day,cloud,night]){tex.wrapS=T.RepeatWrapping;tex.anisotropy=Math.min(4,limit);}
  const surface=new T.Mesh(new T.SphereGeometry(5,128,80),new T.ShaderMaterial({
    vertexShader:globeVertex,uniforms:{day:{value:day},cloud:{value:cloud},night:{value:night},sun:{value:sun},drift:{value:0}},
    fragmentShader:`
    uniform sampler2D day,cloud,night; uniform vec3 sun; uniform float drift;
    varying vec2 coords; varying vec3 N,world;
    void main(){
      vec3 n=normalize(N),v=normalize(cameraPosition-world);
      float l=dot(n,sun),light=smoothstep(-.16,.75,l);
      vec3 land=texture2D(day,coords).rgb;
      float water=smoothstep(.016,.14,land.b-land.r);
      vec3 albedo=mix(land*vec3(.98,1.04,1.05),land*vec3(.56,.85,1.0)+vec3(.015,.040,.075),water);
      float shade=texture2D(cloud,coords+vec2(drift-.0028,.0014)).r;
      vec3 c=albedo*mix(vec3(.055,.091,.16),vec3(.98,1.0,.98),light)*(1.-.27*shade);
      float gloss=pow(max(0.,dot(n,normalize(v+sun))),37.);
      c+=water*gloss*vec3(.28,.32,.35);
      vec3 lamps=texture2D(night,coords).rgb;
      c+=vec3(.98,.62,.30)*pow(max(lamps.r-.12,0.),1.3)*(1.-smoothstep(-.22,.21,l))*.86;
      float rim=pow(1.-max(0.,dot(v,n)),4.1);
      c+=vec3(.13,.39,.58)*rim*(.24+.80*light);
      gl_FragColor=vec4(c,1.);
    }`
  }));globe.add(surface);
  const vapour=new T.Mesh(new T.SphereGeometry(5.016,112,72),new T.ShaderMaterial({
    transparent:true,depthWrite:false,uniforms:{map:{value:cloud},sun:{value:sun}},
    vertexShader:globeVertex,fragmentShader:`
    uniform sampler2D map;uniform vec3 sun;varying vec2 coords;varying vec3 N,world;
    void main(){
      float w=texture2D(map,coords).r;
      float l=smoothstep(-.18,.80,dot(normalize(N),sun));
      vec3 c=mix(vec3(.10,.16,.27),vec3(.94,.965,1.),l);
      gl_FragColor=vec4(c,pow(w,1.20)*.90);
    }`
  }));vapour.renderOrder=2;globe.add(vapour);
  const air=new T.Mesh(new T.SphereGeometry(5.049,128,80),new T.ShaderMaterial({
    side:T.BackSide,transparent:true,depthWrite:false,uniforms:{sun:{value:sun}},
    vertexShader:globeVertex, fragmentShader:`
    uniform vec3 sun;varying vec2 coords;varying vec3 N,world;
    void main(){
      vec3 n=normalize(N),v=normalize(cameraPosition-world);
      float depth=max(0.,-dot(n,v)), daylight=smoothstep(-.45,.7,dot(n,sun));
      float a=smoothstep(.014,.173,depth)*(.09+.64*daylight);
      gl_FragColor=vec4(mix(vec3(.12,.38,.67),vec3(.37,.82,1.),daylight),a);
    }`
  }));air.renderOrder=1;globe.add(air);
  return {globe,rotate(t){
    surface.rotation.y=2.23+t*.028;vapour.rotation.y=surface.rotation.y+t*.0019;
    surface.material.uniforms.drift.value=-t*.0019/(Math.PI*2);
  },textures:[day,cloud,night]};
}
export function makeSpace(scene) {
  let seed=792319;
  const rand=()=>{seed=(Math.imul(seed,1664525)+1013904223)>>>0;return seed/4294967296;};
  const positions=[], sizes=[], intensities=[],temp=[];
  for(let i=0;i<3500;i++){
    const z=-58-110*rand(),x=(rand()-.5)*(-z)*2.45,y=(rand()-.5)*(-z)*1.45;
    positions.push(x,y,z);sizes.push(1.22+Math.pow(rand(),5)*5.15);
    intensities.push(.36+.63*Math.pow(rand(),1.65));temp.push(rand());
  }
  const geo=new T.BufferGeometry();
  for(const [name,data,size] of [['position',positions,3],['radius',sizes,1],['power',intensities,1],['hue',temp,1]])
    geo.setAttribute(name,new T.Float32BufferAttribute(data,size));
  const mat=new T.ShaderMaterial({transparent:true,depthWrite:false,uniforms:{clock:{value:0},idleGlow:{value:0},dpr:{value:1}},
    vertexShader:`
    uniform float clock,dpr,idleGlow;attribute float radius,power,hue;varying float lum,tint,beacon;
    void main(){vec4 p=modelViewMatrix*vec4(position,1.);
      float wave=.5+.5*sin(clock*(1.15+hue*1.5)+position.x);
      beacon=smoothstep(4.,6.3,radius);
      lum=mix(power,1.,beacon)*mix(.89+.11*wave,.48+.52*wave,idleGlow);
      tint=hue;
      gl_Position=projectionMatrix*p;gl_PointSize=max(1.,radius*(1.+beacon*(.24+.34*wave))*dpr*clamp(103./(-p.z),.58,1.3));
    }`,fragmentShader:`
    varying float lum,tint,beacon;void main(){float r=length(gl_PointCoord-.5)*2.;
      vec2 xy=abs(gl_PointCoord*2.-1.);
      float rays=exp(-22.*min(xy.x,xy.y))*pow(max(1.-max(xy.x,xy.y),0.),1.4);
      float a=max(exp(-mix(2.4,7.,beacon)*r*r),beacon*rays)*(1.-smoothstep(.76,1.,r));
      gl_FragColor=vec4(mix(vec3(.72,.84,1.),vec3(1.,.89,.77),tint),lum*a);}`});
  const stars=new T.Points(geo,mat);stars.renderOrder=-2;scene.add(stars);
  // Low-frequency sky colour is isolated so the WebGL canvas can also return true alpha.
  const sky=new T.Mesh(new T.PlaneGeometry(2,2),new T.ShaderMaterial({depthTest:false,depthWrite:false,
    vertexShader:'varying vec2 p;void main(){p=uv;gl_Position=vec4(position.xy,.99999,1.);}',
    fragmentShader:`varying vec2 p;
    void main(){
      float dust=exp(-4.*abs(p.y-.80+.42*p.x))*.61;
      float pool=exp(-3.4*length((p-vec2(.21,.53))*vec2(1.3,1.)));
      float lane=p.y-.76+.39*p.x+.021*sin(8.*p.x);
      float band=exp(-lane*lane*42.);
      float mist=.45+.23*sin(17.*p.x+cos(p.y*13.))+.17*sin(p.x*36.+p.y*21.)+.08*cos(p.x*79.-p.y*65.);
      float dither=fract(sin(dot(gl_FragCoord.xy,vec2(12.9898,78.233)))*43758.5453)/255.;
      vec3 c=vec3(.016,.026,.061)+dust*vec3(.021,.028,.047)+pool*vec3(.016,.028,.042);
      gl_FragColor=vec4(c+dither+band*mist*vec3(.017,.019,.027),1.);}`}));
  sky.frustumCulled=false;sky.renderOrder=-10;scene.add(sky);
  return {sky,stars,uniforms:mat.uniforms};
}
export function lightStudio(scene,renderer) {
  const key=new T.DirectionalLight('#ffebc6',2.25);key.name='StudioKey';key.castShadow=true;key.shadow.mapSize.set(1024,1024);key.shadow.bias=-.00095;key.position.set(-8,9,7);scene.add(key);
  const fill=new T.DirectionalLight('#a7c5ff',.62);fill.position.set(6,2,6);scene.add(fill);
  const rim=new T.DirectionalLight('#b0ddff',2.40);rim.position.set(3,6,-8);scene.add(rim);
  scene.add(new T.HemisphereLight('#d9e8ff','#8d7781',.71));
  // A tiny original studio reflection map gives the gold a broad highlight at the silhouette.
  const w=96,h=48, data=new Float32Array(w*h*4);
  const softbox=(a,b,x,y,wx,wy)=>Math.exp(-(((a-x)/wx)**2+((b-y)/wy)**2));
  for(let y=0;y<h;y++)for(let x=0;x<w;x++){
    const a=x/w,b=y/h, k=softbox(a,b,.22,.70,.105,.105)*5.8, f=softbox(a,b,.73,.59,.06,.17)*2.3;
    const i=(y*w+x)*4;data.set([.13+k+f*.75,.145+k*.91+f*.90,.19+k*.78+f,1],i);
  }
  const tex=new T.DataTexture(data,w,h,T.RGBAFormat,T.FloatType);tex.mapping=T.EquirectangularReflectionMapping;tex.needsUpdate=true;
  const generator=new T.PMREMGenerator(renderer),target=generator.fromEquirectangular(tex);
  scene.environment=target.texture;scene.environmentIntensity=.45;
  generator.dispose();tex.dispose();return target;
}

