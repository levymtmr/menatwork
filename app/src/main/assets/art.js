'use strict';
(() => {
  const display=document.getElementById('city'),c=display.getContext('2d');
  const map=document.getElementById('routeCanvas'),m=map.getContext('2d');
  const green='#92ee73',dim='#37582d',bg='#09170b';
  const reduced=window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  function path(points,color=green,width=2){c.strokeStyle=color;c.lineWidth=width;c.lineJoin='round';c.lineCap='round';c.beginPath();points.forEach((p,i)=>i?c.lineTo(p[0],p[1]):c.moveTo(p[0],p[1]));c.stroke();}
  function shape(draw,fill=bg,stroke=green,width=2){c.beginPath();draw();c.fillStyle=fill;c.fill();c.strokeStyle=stroke;c.lineWidth=width;c.lineJoin='round';c.stroke();}
  function rect(x,y,w,h,color){c.fillStyle=color;c.fillRect(x,y,w,h);}
  function label(text,x,y,color=green,size=8){c.fillStyle=color;c.font=`${size}px monospace`;c.fillText(text,x,y);}
  function worker(swing,working){
    // Original monochrome mechanic mascot, helmet, overalls and work boots.
    shape(()=>{c.moveTo(173,107);c.lineTo(189,107);c.lineTo(187,145);c.quadraticCurveTo(183,152,167,150);c.lineTo(165,146);c.lineTo(173,139);c.closePath();},'#193b17');
    shape(()=>{c.moveTo(191,108);c.lineTo(206,107);c.lineTo(209,139);c.quadraticCurveTo(222,142,219,150);c.lineTo(200,151);c.lineTo(196,143);c.closePath();},'#193b17');
    path([[166,145],[184,145]],green,1);path([[202,145],[218,145]],green,1);
    shape(()=>{c.moveTo(172,66);c.quadraticCurveTo(190,58,209,68);c.lineTo(215,104);c.quadraticCurveTo(196,119,170,108);c.closePath();},'#153513');
    path([[181,66],[183,86],[201,86],[202,66]],green,2);path([[183,72],[182,105]],green,1);path([[201,74],[204,105]],green,1);
    shape(()=>{c.rect(184,86,17,15);},'#1d4418',green,1);path([[187,91],[198,91]],green,1);
    shape(()=>{c.moveTo(173,71);c.quadraticCurveTo(158,72,156,92);c.quadraticCurveTo(155,106,169,106);c.lineTo(173,98);c.quadraticCurveTo(165,99,166,93);c.lineTo(180,80);c.closePath();},'#102a10');
    shape(()=>{c.ellipse(170,100,7,6,0,0,Math.PI*2);},'#275520');
    path([[168,97],[168,103]],green,1);path([[172,97],[172,103]],green,1);
    // Face in profile: no copied game characters or game artwork.
    shape(()=>{c.moveTo(177,37);c.quadraticCurveTo(200,32,203,45);c.lineTo(208,50);c.lineTo(202,53);c.quadraticCurveTo(202,62,188,63);c.lineTo(186,68);c.lineTo(178,64);c.lineTo(180,56);c.quadraticCurveTo(171,52,177,37);},'#193d16');
    c.fillStyle=green;c.fillRect(196,45,2.5,3);path([[193,56],[199,56],[201,54]],green,1.5);
    shape(()=>{c.moveTo(171,38);c.quadraticCurveTo(170,20,187,20);c.quadraticCurveTo(204,21,205,37);c.closePath();},'#244f1d');
    shape(()=>{c.rect(168,36,42,5);},'#244f1d');path([[185,22],[185,34]],green,1.5);path([[190,22],[190,34]],dim,1);
    // Raised arm and hammer descend together toward the anvil.
    const hx=240-12*swing,hy=80-34*swing,ex=225-5*swing,ey=91-29*swing;
    shape(()=>{c.moveTo(207,69);c.quadraticCurveTo(217,64,ex+4,ey-5);c.lineTo(hx+1,hy-5);c.lineTo(hx+6,hy+2);c.lineTo(ex+2,ey+6);c.quadraticCurveTo(215,ey+7,206,80);c.closePath();},'#173715');
    shape(()=>{c.ellipse(hx+2,hy,6,5,0,0,Math.PI*2);},'#2a5820');
    const tx=257-4*swing,ty=106-62*swing;
    path([[hx+1,hy],[tx,ty]],green,4);path([[hx+1,hy],[tx,ty]],'#244d1c',1.5);
    c.save();c.translate(tx,ty);c.rotate(-swing*.8);shape(()=>{c.rect(-14,-7,28,11);},'#315e24');path([[-8,-5],[-8,2]],green,1);c.restore();
    // Anvil, bench and the outline of the part being worked.
    shape(()=>{c.moveTo(233,115);c.lineTo(271,115);c.lineTo(278,111);c.lineTo(277,119);c.lineTo(261,124);c.lineTo(261,133);c.lineTo(271,138);c.lineTo(238,138);c.lineTo(247,132);c.lineTo(247,124);c.lineTo(233,121);c.closePath();},'#22471b');
    path([[232,112],[250,112],[253,115]],green,2);path([[231,142],[281,142]],green,3);path([[237,143],[235,154]],green,2);path([[274,143],[277,154]],green,2);
    if(working&&swing<.13){[[278,100,284,96],[283,107,291,107],[276,94,278,87],[266,97,266,90]].forEach(p=>path([[p[0],p[1]],[p[2],p[3]]],green,1.5));}
  }
  function render(seconds){
    c.clearRect(0,0,400,180);rect(0,0,400,180,bg);
    for(let x=15;x<400;x+=16)for(let y=15;y<175;y+=16)rect(x,y,1,1,'#24411e');
    path([[15,42],[83,42],[103,63],[151,63]],dim,1);path([[15,144],[92,144],[113,122],[151,122]],dim,1);
    path([[285,43],[309,43],[323,29],[384,29]],dim,1);path([[292,135],[318,135],[330,147],[384,147]],dim,1);
    for(const [x,y]of [[151,63],[151,122],[285,43],[292,135]]){c.strokeStyle=dim;c.strokeRect(x-2,y-2,4,4);}
    path([[143,161],[291,161]],green,1);path([[149,165],[149,169],[287,169],[287,165]],dim,1);
    const phase=document.documentElement.dataset.phase||'idle',working=phase==='work';
    const swing=working&&!reduced?(Math.cos(seconds*5)+1)/2:.85;
    worker(swing,working);
    label('MW / 04',16,62,green,10);label('FIELD UNIT',16,75,'#608e4b',7);
    label('REGISTRO',325,50,'#608e4b',8);label('LOCAL',325,61,'#608e4b',8);
    const names={idle:'EM ESPERA',work:'EM SERVIÇO',pause:'EM PAUSA',commute:'EM TRAJETO',return:'RETORNO'};
    label(names[phase]||names.idle,16,128,green,8);
    label(working?'[ ATIVO ]':'[ STANDBY ]',320,128,green,8);
    // Static instrument ticks are part of the illustration, not sensor readings.
    for(let n=0;n<7;n++){path([[20+n*6,91],[20+n*6,96+(n%2)*3]],dim,1);path([[327+n*6,86],[327+n*6,93+(n%2)*3]],dim,1);}
  }
  function renderMap(){
    m.fillStyle=bg;m.fillRect(0,0,360,80);m.lineWidth=.7;m.strokeStyle='#24421f';
    for(let n=0;n<6;n++){m.beginPath();m.moveTo(-10,n*18-25);m.bezierCurveTo(70,n*12,170,n*22+5,370,n*19-35);m.stroke();}
    for(let x=0;x<360;x+=30){m.fillStyle=dim;m.fillRect(x,0,1,3);m.fillRect(x,77,1,3);}
    m.setLineDash([4,4]);m.strokeStyle=green;m.lineWidth=1.5;m.beginPath();m.moveTo(38,55);m.lineTo(100,55);m.lineTo(138,34);m.lineTo(245,34);m.lineTo(283,21);m.lineTo(322,21);m.stroke();m.setLineDash([]);
    // Home and workplace, with a moving-person location marker drawn statically.
    m.strokeStyle=green;m.lineWidth=1.5;m.beginPath();m.moveTo(21,48);m.lineTo(34,36);m.lineTo(47,48);m.moveTo(25,44);m.lineTo(25,61);m.lineTo(43,61);m.lineTo(43,44);m.moveTo(31,61);m.lineTo(31,51);m.lineTo(37,51);m.lineTo(37,61);m.stroke();
    m.strokeRect(313,10,20,28);m.strokeRect(318,5,10,5);for(let x=318;x<330;x+=6)for(let y=15;y<30;y+=7)m.strokeRect(x,y,3,3);
    m.fillStyle=green;m.beginPath();m.moveTo(182,28);m.lineTo(190,36);m.lineTo(182,44);m.lineTo(174,36);m.closePath();m.fill();m.fillStyle=bg;m.fillRect(180,34,4,4);
  }
  render(0);renderMap();let last=0,lastPhase='';
  function animate(t){const phase=document.documentElement.dataset.phase||'idle';if(t-last>120&&!document.hidden&&!document.getElementById('today').classList.contains('hidden')&&(phase!==lastPhase||phase==='work'&&!reduced)){render(t/1000);last=t;lastPhase=phase;}requestAnimationFrame(animate);}requestAnimationFrame(animate);
})();
