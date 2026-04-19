// Storage e estado local
// Extraido de index.html para organizar o frontend sem mudar a arquitetura global.

function defaultYieldRate(type){return type==='savings'?0.55:type==='investment'?0.8:0;}
function normalizeAccount(a){
  const type=a.type||'checking';
  const raw=a.yieldRate ?? a.yield_rate;
  const yieldRate=raw===undefined||raw===null||raw===''?defaultYieldRate(type):parseFloat(raw)||0;
  return {
    ...a,
    type,
    balance:parseFloat(a.balance)||0,
    yieldRate,
    yieldType:a.yieldType||a.yield_type||(type==='investment'?'cdi_pct':'manual'),
    yieldVal:parseFloat(a.yieldVal ?? a.yield_val ?? (type==='investment'?100:yieldRate))||0,
    calcBase:a.calcBase||a.calc_base||'du',
    startDate:a.startDate||a.start_date||'',
    note:a.note||''
  };
}
function normalizeState(st){
  const state=st||{};
  return {
    transactions:state.transactions||[],
    budgets:state.budgets||[],
    goals:state.goals||[],
    accounts:(state.accounts||[]).map(normalizeAccount)
  };
}
function loadLocal(){const r=localStorage.getItem(LK);return normalizeState(r?JSON.parse(r):{transactions:[],budgets:[],goals:[],accounts:[]});}
function saveLocal(){localStorage.setItem(LK,JSON.stringify(S));}
