'use strict';
(() => {
  const city = document.getElementById('city'), ctx = city.getContext('2d');
  const map = document.getElementById('routeCanvas'), m = map.getContext('2d');
  ctx.imageSmoothingEnabled = false;
  m.imageSmoothingEnabled = false;
  let seed = 731;
  function rand() { seed = (seed * 1664525 + 1013904223) >>> 0; return seed / 4294967296; }
  const buildings = [];
  for (let x = 0; x < 400;) {
    const width = 13 + Math.floor(rand() * 24), height = 24 + Math.floor(rand() * 62);
    buildings.push({x, width, height, shade: rand() > .5 ? '#141934' : '#171329', pink: rand() > .45});
    x += width + 3;
  }
  const rain = Array.from({length: 65}, () => ({x: Math.floor(rand() * 420), y: Math.floor(rand() * 160), speed: 1 + rand() * 2}));
  function rect(x,y,w,h,color) { ctx.fillStyle=color;ctx.fillRect(Math.round(x),Math.round(y),w,h); }
  function poly(points,color) {
    ctx.fillStyle=color;ctx.beginPath();points.forEach(([x,y],i)=>i?ctx.lineTo(x,y):ctx.moveTo(x,y));ctx.closePath();ctx.fill();
  }
  // Three-pose pixel worker: raised hammer, downswing and impact.
  function worker(x, y, time) {
    const phase = Math.floor(time / 4) % 3;
    const working = document.documentElement.dataset.phase === 'work';
    const pose = working ? phase : 0;
    rect(x-7,y+39,75,3,'#073b51');
    rect(x+5,y+26,8,16,'#172d53');rect(x+17,y+26,8,16,'#243961');
    rect(x+1,y+40,13,4,'#060918');rect(x+17,y+40,14,4,'#060918');
    rect(x+3,y+9,23,20,'#d62ca7');rect(x+7,y+14,15,3,'#00f5ec');rect(x+11,y+17,5,10,'#89237f');
    rect(x+8,y-5,15,14,'#d8a18b');rect(x+6,y-10,19,7,'#00c6d6');rect(x+3,y-4,25,3,'#03eff0');
    rect(x+17,y+1,7,3,'#101a38');rect(x+1,y+12,6,12,'#f5b59c');
    // Bench and anvil.
    rect(x+36,y+19,25,5,'#a4bed8');rect(x+43,y+24,11,5,'#577393');rect(x+38,y+29,22,4,'#c0c6e4');
    rect(x+33,y+34,33,5,'#6a2c73');rect(x+36,y+39,5,9,'#253650');rect(x+59,y+39,5,9,'#253650');
    if (pose === 0) {
      rect(x+24,y+8,7,6,'#f5b59c');rect(x+28,y-5,5,16,'#f5b59c');
      rect(x+30,y-16,3,15,'#c79b78');rect(x+25,y-20,15,7,'#c6d9e9');rect(x+36,y-18,4,5,'#00f5ec');
    } else if (pose === 1) {
      rect(x+24,y+9,13,6,'#f5b59c');rect(x+34,y+4,4,11,'#c79b78');rect(x+31,y,15,7,'#c6d9e9');
    } else {
      rect(x+24,y+11,16,5,'#f5b59c');rect(x+38,y+12,4,8,'#c79b78');rect(x+36,y+14,16,7,'#c6d9e9');
      [[54,12,3,3],[58,7,2,3],[60,17,4,2],[47,5,2,4],[54,3,2,2]].forEach(a=>rect(x+a[0],y+a[1],a[2],a[3],'#ffd16b'));
    }
  }
  function cityFrame(time) {
    rect(0,0,400,156,'#080d23');
    for(let y=0;y<94;y+=3) rect(0,y,400,3,`rgb(${8+Math.floor(y/9)},${13+Math.floor(y/9)},${35+Math.floor(y/4)})`);
    buildings.forEach((b,i)=>{
      rect(b.x,95-b.height,b.width,b.height,b.shade);rect(b.x,95-b.height,b.width,2,'#26335a');
      if(i%3===0) {rect(b.x+4,87-b.height,2,8,'#2e3d6b');rect(b.x+4,85-b.height,2,2,'#fa27c5');}
      for(let xx=b.x+4;xx<b.x+b.width-3;xx+=6)for(let yy=100-b.height;yy<89;yy+=8) {
        if((xx+yy+i)%5!==0) rect(xx,yy,3,4,((xx+yy)%4===0)?'#713176':(b.pink?'#b42b9a':'#167b9d'));
      }
    });
    // Central tower and foreground architecture, drawn at integer pixel positions.
    rect(194,27,13,64,'#172d50');rect(192,35,17,3,'#00b9d0');rect(198,15,3,12,'#17496c');rect(198,12,3,4,'#fa27c5');
    for(let y=43;y<88;y+=8)rect(197,y,7,2,'#00c3df');
    poly([[0,86],[160,87],[85,156],[0,156]],'#17122a');
    poly([[240,87],[400,78],[400,156],[311,156]],'#171329');
    poly([[161,87],[239,87],[349,156],[54,156]],'#0c1528');
    poly([[161,87],[164,87],[61,156],[57,156]],'#00bac7');
    poly([[237,87],[240,87],[347,156],[343,156]],'#b324b7');
    for(let y=96;y<156;y+=11) {
      const half=Math.round((y-84)*1.8);rect(200-half,y,half*2,1,'#18223a');
    }
    for(let i=0;i<4;i++){const y=94+i*16;rect(198-i,y,2+i,4+i*2,'#416f93');}
    rect(0,20,45,81,'#111129');rect(5,29,32,43,'#4f174c');rect(7,31,28,39,'#1b1233');
    rect(6,29,31,2,'#fa27c5');rect(6,70,31,2,'#fa27c5');rect(5,29,2,43,'#fa27c5');rect(36,29,2,43,'#fa27c5');
    [[13,38,15,3],[13,45,3,13],[25,45,3,13],[13,61,15,3],[18,49,8,3]].forEach(a=>rect(...a,'#f143c4'));
    rect(361,9,39,91,'#10162d');rect(365,22,35,2,'#00b5cc');rect(365,27,35,2,'#106b88');
    for(let y=35;y<84;y+=10) {rect(368,y,8,5,'#165772');rect(383,y,10,5,'#bd248f');}
    rect(321,58,31,20,'#12546c');rect(323,60,27,16,'#0c2239');rect(329,63,14,2,'#00eee6');rect(326,67,20,5,'#00a6b6');
    poly([[39,87],[185,80],[185,83],[39,94]],'#ef27b4');poly([[229,80],[400,48],[400,52],[229,84]],'#f230b7');
    for(let i=0;i<18;i++){const x=i*25;rect(x,110+(i%4)*8,10+(i%3)*7,1,i%2?'#324773':'#6c1c68');}
    rect(150,137,116,4,'#153449');
    worker(166, 91, time);
    ctx.globalAlpha=.45;
    rain.forEach(r=>{let y=(r.y+time*r.speed)%165;rect(r.x-y*.12,y,1,5,'#426b9b');});
    ctx.globalAlpha=1;
    for(let y=0;y<156;y+=4)rect(0,y,400,1,'#03061622');
  }
  function mapFrame() {
    m.fillStyle='#0a0e24';m.fillRect(0,0,360,80);
    seed=81;
    for(let x=0;x<360;x+=24)for(let y=0;y<80;y+=22){m.fillStyle=rand()>.5?'#20203b':'#18182f';m.fillRect(x+2,y+2,18,16);m.fillStyle=rand()>.5?'#674082':'#144c68';m.fillRect(x+5,y+5,3,3);}
    m.strokeStyle='#00f5ec';m.lineWidth=3;m.beginPath();m.moveTo(23,59);m.lineTo(91,59);m.lineTo(91,41);m.lineTo(210,41);m.lineTo(230,20);m.lineTo(333,20);m.stroke();
    [ [23,59], [333,20] ].forEach(([x,y])=>{m.fillStyle='#fa27c5';m.fillRect(x-5,y-13,10,10);m.fillRect(x-2,y-3,4,5);m.fillStyle='#151429';m.fillRect(x-2,y-10,4,4);});
    m.fillStyle='#00f5ec';m.fillRect(174,29,8,8);m.fillRect(171,37,14,10);m.fillRect(172,47,4,7);m.fillRect(181,47,4,7);m.fillStyle='#fa27c5';m.fillRect(171,26,14,4);
  }
  cityFrame(0);mapFrame();
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if(!reduced) {let last=0;function animate(t){if(t-last>120&&!document.hidden&&!document.getElementById('today').classList.contains('hidden')){cityFrame(t/45);last=t;}requestAnimationFrame(animate);}requestAnimationFrame(animate);}
})();
