export function bindRig(root) {
  const get=n=>root.getObjectByName(n);
  const pair=n=>['L','R'].map(side=>get(n+side));
  return {root,body:get('Blazer'),head:get('Topeng'),eyes:pair('Eye'),glints:pair('Spark'),
    ribbons:pair('Ribbon'),arms:pair('Arm').map((arm,i)=>({arm,elbow:pair('Elbow')[i],palm:pair('Hand')[i]}))};
}
// Author the intro in GSAP, then bake these local pivots into the portable GLB clip.
// Idle is sampled from the held pose, so pausing or scrubbing never accumulates offsets.
export function poseRig(mascot,pose,idle=0) {
  const breath=Math.sin(idle*1.75),sway=Math.sin(idle*.92);
  mascot.head.rotation.set(pose.pitch+.009*breath,pose.yaw+.012*sway,pose.tilt+.009*sway);
  mascot.body.rotation.y=-.09;
  for(let i=0;i<2;i++){
    const s=i?1:-1,arm=mascot.arms[i],lift=i?pose.shoulderR:pose.shoulderL;
    arm.elbow.rotation.set(-.085,-s*.12,s*((i?pose.foldR:pose.foldL)+.022*sway));
    arm.arm.rotation.set(-.08,s*.065,s*(lift-.45+.012*Math.sin(idle*1.5+i)*Math.min(idle,1)));
    arm.palm.rotation.y=s*(.22*lift+.48*(i?pose.wristR:pose.wristL));
    arm.palm.rotation.z=s*(-.74+(i?pose.wristR:pose.wristL)+.013*sway);
    mascot.ribbons[i].rotation.set(.2*pose.lag,0,s*(pose.lag*(i?.68:.94)+.014*sway));
    const phase=(idle+3.1)%5.8,blink=1-Math.pow(Math.max(0,1-Math.abs(phase-5.45)/.125),1.28)*.95;
    mascot.eyes[i].scale.y=blink*(1+.065*pose.shine);
    mascot.glints[i].scale.setScalar(Math.max(.001,pose.shine*(1+.035*breath)));
    mascot.glints[i].rotation.z=(1-pose.shine)*.88+s*.12;
  }
}

