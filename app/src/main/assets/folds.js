'use strict';
(() => {
 const state=new Map(),openers=new WeakMap();
 function enhance(panel,id,compact=false,record=false){if(panel.dataset.foldBound)return;panel.dataset.foldBound='1';panel.classList.add('foldable-panel');const children=[...panel.children],body=document.createElement('div');body.className='fold-body';body.id='fold-'+id.replace(/[^a-zA-Z0-9_-]/g,'-');
  for(const child of children){const keep=record&&child.matches('.history-top,h3,.history-net,.record-date,.billing-badge,.record-client,.skill-level,.balance-top,.balance-body,.corner-detail');if(!keep)body.append(child);}
  // Keep the title of regular panels visible while moving only their controls.
  if(!record){const title=body.querySelector(':scope > h3,:scope > .panel-title,:scope > label[for]');if(title)panel.append(title);}
  const button=document.createElement('button');button.type='button';button.className='fold-toggle';button.setAttribute('aria-controls',body.id);panel.append(button,body);
  function paint(){const open=state.has(id)?state.get(id):!compact;body.classList.toggle('hidden',!open);panel.classList.toggle('is-folded',!open);button.setAttribute('aria-expanded',String(open));button.textContent=open?'− RECOLHER':'+ EXPANDIR';}
  button.onclick=()=>{state.set(id,button.getAttribute('aria-expanded')!=='true');paint();};openers.set(panel,()=>{state.set(id,true);paint();});paint();
 }
 const balance=document.querySelector('#today .balance');enhance(balance,'balance',true,true);
 window.FoldUI={expand(panel){openers.get(panel)?.();},record(panel,id){enhance(panel,id,true,true);},enhance};
 for(const [id,panel]of [...document.querySelectorAll('#settings .panel,#today .panel.breakdown,#today .panel.route,#activeSkillsPanel,#invoices > .panel.settings-panel')].entries())enhance(panel,'static-'+id,false,false);
})();
