'use strict';
(() => {
  const $=id=>document.getElementById(id);
  let latest={stage:'idle',currentVersion:'1.10.6',message:'Toque para procurar uma nova versão no GitHub.',progress:0};
  let requested=false;
  function command(action){
    if(typeof Android==='undefined'){$('updateMessage').textContent='Instale o APK para verificar e instalar atualizações.';return;}
    Android.command(action,'{}');
  }
  window.AppUpdateUI={render(update){
    if(update)latest=update;
    $('updateVersion').textContent=latest.currentVersion;
    $('appVersionLabel').textContent='MENATWORK / '+latest.currentVersion;
    $('updateMessage').textContent=latest.message;
    const busy=latest.stage==='checking'||latest.stage==='downloading';
    $('updateButton').disabled=busy;
    $('updateButton').textContent=busy?(latest.stage==='checking'?'PROCURANDO ATUALIZAÇÃO…':'BAIXANDO · '+latest.progress+'%'):latest.stage==='ready'?'INSTALAR ATUALIZAÇÃO':'↻ ATUALIZAR APLICATIVO';
    $('updateProgress').classList.toggle('hidden',latest.stage!=='downloading');$('updateProgress').value=latest.progress||0;
    $('updateNotes').textContent=latest.notes||'';$('updateNotes').classList.toggle('hidden',!latest.notes);
    if(requested&&latest.stage==='ready'){requested=false;command('installUpdate');}
    if(latest.stage==='error'||latest.stage==='current')requested=false;
  }};
  $('updateButton').onclick=()=>{
    if(latest.stage==='ready'){command('installUpdate');return;}
    requested=true;command('checkUpdate');
  };
})();
