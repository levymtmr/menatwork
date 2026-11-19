'use strict';
(function(root){
 function date(t){const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;}
 function summary(history,session,totals,config,now){
  const start=new Date(now);start.setHours(0,0,0,0);start.setDate(start.getDate()-(start.getDay()+6)%7);const end=new Date(start);end.setDate(end.getDate()+7);const from=date(start),to=date(end),groups={};
  const rows=history.filter(r=>!r.deleted).slice();if(session?.phase)rows.push({...session,config:session.config||config,totals});
  for(const r of rows){const day=r.workDate||date(r.workStartedAt||r.startedAt);if(day<from||day>=to)continue;const currency=r.config?.currency||'AUD',g=groups[currency]||(groups[currency]={workedMs:0,gross:0,net:0,days:{}}),t=r.totals||{},ms=Number(t.workedMs??r.workedMs??0);g.workedMs+=ms;g.gross+=Number(t.gross||0);g.net+=Number(t.net||0);g.days[day]=(g.days[day]||0)+ms;}
  return {from,to,groups};
 }
 root.Weekly={summary};if(typeof module!=='undefined')module.exports=root.Weekly;
})(typeof window!=='undefined'?window:globalThis);
