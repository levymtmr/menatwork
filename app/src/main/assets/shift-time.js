'use strict';
(() => {
  function format(shift){
    if(shift.entryMode==='times'&&shift.startTime&&shift.endTime)return shift.startTime+' – '+shift.endTime+(shift.nextDay?' (+1)':'');
    if(shift.timesheetEntry)return shift.timesheetEntry.start+' – '+shift.timesheetEntry.end;
    if(shift.entryMode==='hours')return '';
    const spans=shift.workIntervals||[],from=spans.length?spans[0][0]:shift.workStartedAt,to=spans.length?spans[spans.length-1][1]:shift.endedAt;
    if(!from||!to||to<from)return '';
    const zone=shift.workZone||'Australia/Adelaide';
    const clock=new Intl.DateTimeFormat('en-GB',{hour:'2-digit',minute:'2-digit',hourCycle:'h23',timeZone:zone}),day=new Intl.DateTimeFormat('en-CA',{year:'numeric',month:'2-digit',day:'2-digit',timeZone:zone});
    return clock.format(from)+' – '+clock.format(to)+(day.format(from)!==day.format(to)?' (+1)':'');
  }
  if(typeof module!=='undefined')module.exports={format};else window.ShiftTime={format};
})();
