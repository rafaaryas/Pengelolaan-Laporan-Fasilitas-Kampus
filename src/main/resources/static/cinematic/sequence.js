export const DURATION = 4.5;
export const beats=[
  [0,'Orbit bumi'],[.6,'Mendekati bumi'],[1.9,'Di balik cakrawala'],[2.15,'Cakra muncul'],[3.25,'Salam Cakra']
];
export function makeSequence() {
  const pose={push:0, lift:0, shoulderL:-.95,shoulderR:-.89,wristL:0,wristR:0,
    pitch:.22,tilt:-.1,yaw:-.2,lag:-.21,shine:0, lean:0,foldL:.72,foldR:.84};
  const timeline=window.gsap.timeline({paused:true});
  for(const [at,name] of beats)timeline.addLabel(name,at);
  const to=(at,duration,props,ease='sine.inOut')=>timeline.to(pose,{...props,duration,ease},at);
  to(.60,1.48,{push:1},'power2.inOut');
  to(1.89,.26,{lean:-.069},'sine.in');
  to(2.15,.76,{lift:1.022,lean:.034},'power1.inOut');
  to(2.91,.42,{lift:.998,lean:-.006});
  to(3.33,.47,{lift:1,lean:0});
  to(2.38,.57,{shoulderL:.58},'power2.out');to(2.47,.60,{shoulderR:.51},'power2.out');
  to(2.96,.34,{shoulderL:.30});to(3.07,.37,{shoulderR:.28});
  to(3.30,.48,{shoulderL:.34});to(3.44,.41,{shoulderR:.31});
  to(2.36,.27,{foldL:1.04},'sine.inOut');to(2.44,.27,{foldR:1.13},'sine.inOut');
  to(2.63,.44,{foldL:.55},'power2.out');to(2.71,.44,{foldR:.64},'power2.out');
  to(3.07,.62,{foldL:.78});to(3.15,.66,{foldR:.65});
  to(2.34,.66,{pitch:-.089,tilt:.071,yaw:.105},'power2.out');
  to(3.00,.57,{pitch:.026,tilt:-.068,yaw:-.22});to(3.57,.58,{pitch:0,tilt:-.057,yaw:-.19});
  to(2.45,.54,{wristL:.30,wristR:.20,lag:.25});to(3.02,.36,{wristL:-.08,wristR:-.065,lag:-.077});
  to(3.38,.57,{wristL:0,wristR:0,lag:.013});to(3.95,.55,{lag:0});
  to(3.27,.19,{shine:1.68},'power2.out');to(3.46,.37,{shine:1.12});to(3.83,.42,{shine:1.27});
  return {timeline,pose};
}

