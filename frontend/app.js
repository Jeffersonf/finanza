'use strict';
const APP_VERSION='3.9.1';
const DEFAULT_API_URL='https://finanza-api.onrender.com';
const CK='fz_cfg',LK='fz_local',CCK='fz_cats',VK='fz_view',AVK='fz_avatar';
const RATES_KEY='fz_rates', WIDGET_ORDER_KEY='fz_widget_order';
let cfg={url:'',key:'',mode:'',userName:'',userId:''};
let S={transactions:[],budgets:[],goals:[],accounts:[]};
let custCats=[];
let curTxP='1m-p',curFP='7d',curDt=new Date();
let chartMode='bars',curView='n',catFilter=null;
let editId=null,accEditId=null,qaTyp='expense',qaVal='',qaSelCat='';
const uid=()=>Date.now().toString(36)+Math.random().toString(36).slice(2);
const fmt=n=>'R$ '+Number(n).toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2});
const fmtD=d=>{if(!d)return'';const[y,m,day]=d.substring(0,10).split('-');return`${day}/${m}/${y}`;};
const today=()=>new Date().toISOString().split('T')[0];
const isFut=d=>d>today();
const dDiff=d=>Math.ceil((new Date(d+'T12:00:00')-new Date())/864e5);
const offD=(b,n)=>{const d=new Date(b);d.setDate(d.getDate()+n);return d.toISOString().split('T')[0];};
const addM=(s,n)=>{const d=new Date(s+'T12:00:00');d.setMonth(d.getMonth()+n);return d.toISOString().split('T')[0];};
const addW=(s,n)=>offD(new Date(s),n*7);
const addY=(s,n)=>{const d=new Date(s+'T12:00:00');d.setFullYear(d.getFullYear()+n);return d.toISOString().split('T')[0];};
const MO=['Janeiro','Fevereiro','Março','Abril','Maio','Junho','Julho','Agosto','Setembro','Outubro','Novembro','Dezembro'];
const RATES={cdi:10.40,selic:10.50,tr:0};
const BCATS=[
  {id:'sal',ico:'\u{1F4BC}',name:'Salário',col:'#c8f55a'},
  {id:'frl',ico:'\u{1F5A5}\uFE0F',name:'Freelance',col:'#f5f55a'},
  {id:'inv',ico:'\u{1F4C8}',name:'Investimentos',col:'#5af55a'},
  {id:'oth',ico:'\u{1F516}',name:'Outros',col:'#a78bfa'},
  {id:'mor',ico:'\u{1F3E0}',name:'Moradia',col:'#5af5c8'},
  {id:'ali',ico:'\u{1F37D}\uFE0F',name:'Alimentação',col:'#f5c85a'},
  {id:'tra',ico:'\u{1F697}',name:'Transporte',col:'#5a9ef5'},
  {id:'sau',ico:'\u{1F48A}',name:'Saúde',col:'#f55a9e'},
  {id:'laz',ico:'\u{1F3AC}',name:'Lazer',col:'#c85af5'},
  {id:'edu',ico:'\u{1F4DA}',name:'Educação',col:'#5af55a'},
  {id:'rou',ico:'\u{1F455}',name:'Roupas',col:'#f57c5a'},
  {id:'tec',ico:'\u{1F4BB}',name:'Tecnologia',col:'#5acff5'},
  {id:'ass',ico:'\u{1F4E6}',name:'Assinaturas',col:'#f5a05a'},
  {id:'cls',ico:'\u2753',name:'A classificar',col:'#6b7494'},
];
const CAT_ALIASES={
  Salario:'Salário',
  Alimentacao:'Alimentação',
  Saude:'Saúde',
  Educacao:'Educação',
  Poupanca:'Poupança',
};
const normCatName=n=>CAT_ALIASES[n]||n||'';
const CAT_COLORS=['#5af5c8','#f5705a','#a78bfa','#5a9ef5','#f5c85a','#f55a9e','#4ade80','#f5a05a','#5acff5','#c8f55a','#9e8cff','#ff8c6b'];
function hashStr(s){let h=0;for(let i=0;i<(s||'').length;i++)h=((h<<5)-h+s.charCodeAt(i))|0;return Math.abs(h);}
function cleanColor(c){
  if(!c)return'';
  c=String(c).trim().toLowerCase();
  if(/^#[0-9a-f]{3}$/.test(c))c='#'+c[1]+c[1]+c[2]+c[2]+c[3]+c[3];
  return /^#[0-9a-f]{6}$/.test(c)?c:'';
}
function catColor(name,col){
  const c=cleanColor(col);
  if(!c||['#000000','#111111','#222222','#333333','#666666','#777777','#888888','#999999'].includes(c))return CAT_COLORS[hashStr(name)%CAT_COLORS.length];
  return c;
}
function normalizeCat(c){const name=normCatName(c?.name);return {...c,name,col:catColor(name,c?.col||c?.color)};}
const allCats=()=>[...BCATS,...custCats];
const getCat=n=>normalizeCat(allCats().find(c=>c.name===n)||{ico:'\u{1F516}',name:n,col:catColor(n),custom:true});
function getInitials(name){
  return (name||'Eu').trim().split(/\s+/).slice(0,2).map(p=>p[0]||'').join('').toUpperCase()||'EU';
}
function applyAvatar(){
  const av=document.getElementById('uAvatar');if(!av)return;
  const img=localStorage.getItem(AVK);
  av.classList.toggle('has-photo',!!img);
  av.style.backgroundImage=img?`url("${img}")`:'';
  av.textContent=img?'':getInitials(cfg.userName||'Eu');
  if(img)av.setAttribute('aria-label','Foto do perfil');else av.removeAttribute('aria-label');
}
function changeAvatar(inp){
  const file=inp?.files?.[0];if(!file)return;
  if(!file.type.startsWith('image/')){toast('Escolha uma imagem','error');inp.value='';return;}
  if(file.size>2*1024*1024){toast('Use uma imagem até 2 MB','error');inp.value='';return;}
  const reader=new FileReader();
  reader.onload=e=>{localStorage.setItem(AVK,e.target.result);applyAvatar();toast('Foto atualizada','success');inp.value='';};
  reader.onerror=()=>toast('Não foi possível carregar a foto','error');
  reader.readAsDataURL(file);
}
function removeAvatar(){localStorage.removeItem(AVK);applyAvatar();toast('Foto removida','info');}
function applyTheme(t){
  document.documentElement.dataset.theme=t;
  document.getElementById('thmBtn').textContent=t==='dark'?'\u{1F319}':'\u2600\uFE0F';
  const tog=document.getElementById('thmTog');if(tog)tog.checked=t==='dark';
  const m=document.getElementById('themeColorMeta');if(m)m.content=t==='dark'?'#0a0c10':'#f2f4fb';
  localStorage.setItem('fz_t',t);
  if(cfg?.mode==='api')saveRemoteState().catch(()=>{});
}
function toggleTheme(){applyTheme(document.documentElement.dataset.theme==='dark'?'light':'dark');renderDash();}
function thmFromSet(v){applyTheme(v?'dark':'light');renderDash();}
function initTheme(){applyTheme(localStorage.getItem('fz_t')||(window.matchMedia('(prefers-color-scheme:light)').matches?'light':'dark'));}
function loadCC(){try{custCats=JSON.parse(localStorage.getItem(CCK)||'[]');}catch{custCats=[];}}
function saveCC(){localStorage.setItem(CCK,JSON.stringify(custCats));if(cfg.mode==='api')saveRemoteState().catch(()=>{});}
function addCustCat(){
  const ico=document.getElementById('nCatIco').value.trim()||'\u{1F3F7}\uFE0F';
  const name=normCatName(document.getElementById('nCatNm').value.trim());
  if(!name){toast('Informe o nome','error');return;}
  if(allCats().find(c=>c.name===normCatName(name))){toast('Categoria já existe','error');return;}
  custCats.push({id:uid(),ico,name,col:CAT_COLORS[custCats.length%CAT_COLORS.length],custom:true});
  saveCC();renderCatChips();popCatSels();toast(`"${name}" criada! OK`,'success');
}
function delCustCat(id){custCats=custCats.filter(c=>c.id!==id);saveCC();renderCatChips();popCatSels();}
function renderCatChips(){
  const el=document.getElementById('custCatChips');if(!el)return;
  el.innerHTML=custCats.length?custCats.map(c=>`<div class="cat-chip">${c.ico} ${c.name}<span class="chip-x" onclick="delCustCat('${c.id}')">&times;</span></div>`).join(''):'<span style="font-size:11px;color:var(--mt)">Nenhuma ainda.</span>';
}
function popCatSels(){
  const cats=allCats();
  const opts=cats.map(c=>`<option value="${c.name}">${c.ico} ${c.name}</option>`).join('');
  ['txCat','budCat'].forEach(id=>{const e=document.getElementById(id);if(e)e.innerHTML=opts;});
  renderQACats();
}
function renderQACats(){
  const el=document.getElementById('qaCats');if(!el)return;
  el.innerHTML=allCats().filter(c=>!['Salário','Freelance','Investimentos','Outros'].includes(c.name)).slice(0,10).map(c=>`<div class="qa-cat${qaSelCat===c.name?' sel':''}" onclick="qaSelC('${c.name}')">${c.ico} ${c.name}</div>`).join('');
}
function showSetup(){document.getElementById('setup').classList.add('visible');showS('main');}
function hideSetup(){document.getElementById('setup').classList.remove('visible');}
function showS(s){['Main','Online','NewUser','Reset','Local'].forEach(n=>{const e=document.getElementById('s'+n);if(e)e.style.display=n.toLowerCase()===s?'block':'none';});}
async function doSetup(){
  const url=(document.getElementById('sUrl')?.value.trim()||cfg.url||DEFAULT_API_URL).replace(/\/$/,'');
  const username=document.getElementById('sUser').value.trim();
  const password=document.getElementById('sPass').value;
  const err=document.getElementById('sErr');err.classList.remove('show');
  if(!url||!username||!password){err.textContent='Preencha URL, usuário e senha.';err.classList.add('show');return;}
  const btn=document.getElementById('sBtn'),txt=document.getElementById('sBtnTxt');
  btn.disabled=true;txt.textContent='Conectando...';
  try{
    await fetch(url+'/health');
    const login=await fetch(url+'/api/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username,password})});
    if(!login.ok){const er=await login.json().catch(()=>({}));throw new Error(er.error||'Login inválido');}
    const u=await login.json();
    cfg={url,key:u.api_key,mode:'api',userName:u.name,userId:u.id,loginName:username};
    localStorage.setItem(CK,JSON.stringify(cfg));
    hideSetup();await initApp();toast(`Bem-vindo, ${u.name}! OK`,'success');
  }catch(e){err.textContent='Falha: '+e.message;err.classList.add('show');btn.disabled=false;txt.textContent='Entrar →';}
}
async function createUser(){
  const url=(document.getElementById('nuUrl')?.value.trim()||cfg.url||DEFAULT_API_URL).replace(/\/$/,'');
  const admin=document.getElementById('nuAdmin').value.trim();
  const name=document.getElementById('nuName').value.trim()||'Usuário';
  const username=document.getElementById('nuUser').value.trim();
  const password=document.getElementById('nuPass').value;
  const err=document.getElementById('nuErr');err.classList.remove('show');
  if(!username||!password){err.textContent='Preencha usuário e senha.';err.classList.add('show');return;}
  try{
    const endpoint=admin?'/api/users':'/api/register';
    const headers={'Content-Type':'application/json'};
    if(admin)headers['x-api-key']=admin;
    const r=await fetch(url+endpoint,{method:'POST',headers,body:JSON.stringify({name,username,password})});
    if(!r.ok){const er=await r.json().catch(()=>({}));throw new Error(er.error||'Erro');}
    await r.json();
    document.getElementById('nuRes').style.display='block';
  }catch(e){err.textContent='Erro: '+e.message;err.classList.add('show');}
}
function useNewUserKey(){
  const sUrl=document.getElementById('sUrl');if(sUrl)sUrl.value=(document.getElementById('nuUrl')?.value.trim()||DEFAULT_API_URL);
  document.getElementById('sUser').value=document.getElementById('nuUser').value.trim();
  document.getElementById('sPass').value=document.getElementById('nuPass').value;
  showS('online');
  doSetup();
}
async function resetPassword(){
  const url=(document.getElementById('rpUrl')?.value.trim()||cfg.url||DEFAULT_API_URL).replace(/\/$/,'');
  const username=document.getElementById('rpUser').value.trim();
  const password=document.getElementById('rpPass').value;
  const admin=document.getElementById('rpAdmin').value.trim();
  const err=document.getElementById('rpErr');err.classList.remove('show');
  if(!username||!password||!admin){err.textContent='Preencha usuário, nova senha e chave admin.';err.classList.add('show');return;}
  try{
    const r=await fetch(url+'/api/password-reset',{method:'POST',headers:{'Content-Type':'application/json','x-api-key':admin},body:JSON.stringify({username,password})});
    if(!r.ok){const e=await r.json().catch(()=>({}));throw new Error(e.error||'Erro');}
    document.getElementById('sUser').value=username;
    document.getElementById('sPass').value=password;
    showS('online');
    toast('Senha redefinida. Entrando...','success');
    doSetup();
  }catch(e){err.textContent='Erro: '+e.message;err.classList.add('show');}
}
function startLocal(){cfg={url:'',key:'',mode:'local',userName:'Eu',userId:''};localStorage.setItem(CK,JSON.stringify(cfg));hideSetup();initApp();}
function normalizeBackupData(d){
  const data=d?.app==='Finanza'||d?.version?d:{...d};
  if(!Array.isArray(data.transactions))throw new Error('Arquivo inválido: não parece um backup do Finanza');
  data.transactions=(data.transactions||[]).map(t=>nTx({
    ...t,
    description:t.description||t.desc||'Lançamento',
    account_id:t.account_id||t.accountId||null,
    installment_group:t.installment_group||t.installmentGroup||null,
    installment_num:t.installment_num||t.installmentNum||null,
    installment_total:t.installment_total||t.installmentTotal||null,
    recur_group:t.recur_group||t.recurGroup||null
  })).filter(t=>t.amount>0&&t.date);
  data.budgets=(data.budgets||[]).map(nBud).filter(b=>b.category&&b.limit>0);
  data.goals=(data.goals||[]).map(nGoal).filter(g=>g.name&&g.target>0&&g.deadline);
  data.accounts=(data.accounts||[]).map(normalizeAccount);
  data.categories=(data.categories||data.customCategories||[]).map(nCat);
  const shopping=data.shopping||{lists:data.shoppingLists||[],items:data.shoppingItems||[]};
  data.shopping={lists:(shopping.lists||[]).map(nShopList),items:(shopping.items||[]).map(nShopItem)};
  data.settings=data.settings||{};
  return data;
}
function applyBackupData(data){
  S={transactions:data.transactions,budgets:data.budgets,goals:data.goals,accounts:data.accounts.length?data.accounts:defAccs()};
  custCats=data.categories||[];
  sl=data.shopping?.lists?.length?data.shopping:{lists:[{id:uid(),name:'Mercado',ico:'\u{1F6D2}'}],items:[]};
  slActiveList=data.settings?.activeList||data.settings?.active_list||sl.lists[0]?.id||null;
  localStorage.setItem(LK,JSON.stringify(S));
  localStorage.setItem(CCK,JSON.stringify(custCats));
  localStorage.setItem(SL_KEY,JSON.stringify(sl));
}
function importLocal(inp){
  const f=inp.files[0];if(!f)return;
  const r=new FileReader();
  r.onload=e=>{
    try{
      const data=normalizeBackupData(JSON.parse(e.target.result));
      applyBackupData(data);
      cfg={url:'',key:'',mode:'local',userName:data.user||'Eu',userId:''};
      localStorage.setItem(CK,JSON.stringify(cfg));
      hideSetup();
      initApp();
      toast('Importado: '+data.transactions.length+' transações OK','success');
    }catch(err){
      alert('Erro ao importar: '+err.message+'\n\nVerifique se o arquivo é um backup válido do Finanza.');
    }
  };
  r.readAsText(f,'UTF-8');
}
function importBackupFile(inp){
  const f=inp.files[0];if(!f)return;
  const r=new FileReader();
  r.onload=async e=>{
    try{
      const data=normalizeBackupData(JSON.parse(e.target.result));
      const msg=`Importar ${data.transactions.length} transações, ${data.accounts.length} contas, ${data.budgets.length} orçamentos e ${data.goals.length} metas? Isso substitui os dados atuais desta conta.`;
      if(!confirm(msg)){inp.value='';return;}
      if(cfg.mode==='api'){
        const result=await api('PUT','/api/import',data);
        await loadAll();
        toast(`Importado: ${result.imported?.transactions||S.transactions.length} transações, ${S.accounts.length} contas, ${S.budgets.length} orçamentos, ${S.goals.length} metas`,'success');
      }else{
        applyBackupData(data);
        toast('Backup importado localmente OK','success');
      }
      refreshAll();
    }catch(err){toast('Erro ao importar: '+err.message,'error');}
    finally{inp.value='';}
  };
  r.readAsText(f,'UTF-8');
}
function logout(){if(!confirm('Sair da conta?'))return;localStorage.removeItem(CK);localStorage.removeItem(LK);location.reload();}
function setConn(s){
  const dot=document.getElementById('connDot');if(dot)dot.className='conn-dot '+s;
  const L={online:'Dados sincronizados',offline:'Dados locais',error:'Sem conexao'};
  const lbl=document.getElementById('connLbl');if(lbl)lbl.textContent=L[s]||s;
  document.getElementById('uRole').textContent=cfg.mode==='api'?'Conta online':'Modo local';
}
function openConnModal(){document.getElementById('connUrl').value=cfg.url;document.getElementById('connUser').value=cfg.loginName||'';document.getElementById('connPass').value='';document.getElementById('connModal').classList.add('open');}
async function saveConn(){
  const url=document.getElementById('connUrl').value.trim().replace(/\/$/,'');
  const username=document.getElementById('connUser').value.trim();
  const password=document.getElementById('connPass').value;
  if(!url||!username||!password){toast('Preencha URL, usuário e senha','error');return;}
  try{
    const r=await fetch(url+'/api/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username,password})});
    if(!r.ok){const e=await r.json().catch(()=>({}));throw new Error(e.error||'Login inválido');}
    const u=await r.json();
    cfg={...cfg,url,key:u.api_key,mode:'api',userName:u.name,userId:u.id,loginName:username};localStorage.setItem(CK,JSON.stringify(cfg));
    closeM('connModal');await initApp();toast('Salvo!','success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
function nTx(t){return{id:t.id,type:t.type,desc:t.description||t.desc||'',amount:parseFloat(t.amount),category:normCatName(t.category||'A classificar'),date:(t.date||'').substring(0,10),note:t.note||'',accountId:t.accountId||t.account_id||null,installmentGroup:t.installmentGroup||t.installment_group||null,installmentNum:t.installmentNum||t.installment_num||null,installmentTotal:t.installmentTotal||t.installment_total||null,recurGroup:t.recurGroup||t.recur_group||null,paid:t.paid||false,pending:t.pending||false};}
function nBud(b){return{id:b.id,category:normCatName(b.category),limit:parseFloat(b.limit)};}
function nGoal(g){return{id:g.id,name:g.name,icon:g.icon||'\u{1F3AF}',target:parseFloat(g.target),current:parseFloat(g.current||0),deadline:(g.deadline||'').substring(0,10),desc:g.description||g.desc||'',monthly:parseFloat(g.monthly||0)};}
function nAcc(a){return normalizeAccount({id:a.id,name:a.name,icon:a.icon,type:a.type,balance:a.balance,yieldRate:a.yield_rate,yieldType:a.yield_type,yieldVal:a.yield_val,calcBase:a.calc_base,startDate:(a.start_date||'').substring(0,10),note:a.note});}
function nCat(c){const name=normCatName(c.name);return{id:c.id,ico:c.icon||c.ico||'\u{1F3F7}\uFE0F',name,col:catColor(name,c.color||c.col),custom:true};}
function nShopList(l){return{id:l.id,name:l.name,ico:l.icon||l.ico||'\u{1F6D2}',position:l.position||0};}
function nShopItem(i){return{id:i.id,listId:i.list_id||i.listId,name:i.name,qty:i.qty||'',cat:i.category||i.cat||'\u{1F6D2} Geral',bought:!!i.bought,createdAt:Number(i.created_ms||i.createdAt||Date.now())};}
function defAccs(){return[{id:uid(),name:'Principal',icon:'\u{1F3E6}',type:'checking',balance:0,yieldRate:0,note:''}];}
function asObj(v){return v&&typeof v==='object'&&!Array.isArray(v)?v:{};}
function asArr(v){return Array.isArray(v)?v:[];}
function getAppSettings(){
  return {theme:document.documentElement.dataset.theme||localStorage.getItem('fz_t')||'dark',rates:{cdi:RATES.cdi,selic:RATES.selic},widgetPrefs,widgetOrder,txView:curView,activeList:slActiveList};
}
function applyRemoteSettings(settings={}){
  if(settings.theme)applyTheme(settings.theme);
  const rates=settings.rates||{};
  if(rates.cdi)RATES.cdi=parseFloat(rates.cdi);
  if(rates.selic)RATES.selic=parseFloat(rates.selic);
  widgetPrefs=asObj(settings.widget_prefs||settings.widgetPrefs||widgetPrefs);
  widgetOrder=asArr(settings.widget_order||settings.widgetOrder||widgetOrder);
  if(settings.tx_view||settings.txView)localStorage.setItem(VK,settings.tx_view||settings.txView);
  if(settings.active_list||settings.activeList)slActiveList=settings.active_list||settings.activeList;
}
async function saveRemoteState(){
  if(cfg.mode!=='api')return;
  const shopping=sl?.lists?.length?sl:(()=>{try{return JSON.parse(localStorage.getItem(SL_KEY)||'{}');}catch{return{lists:[],items:[]};}})();
  await api('PUT','/api/state',{accounts:S.accounts,categories:custCats,shopping,settings:getAppSettings()});
}
function persistLocalOrRemote(){
  if(cfg.mode==='api')saveRemoteState().catch(e=>toast('Erro ao salvar estado: '+e.message,'error'));
  else saveLocal();
}
async function loadAll(){
  if(cfg.mode==='local'){S=loadLocal();if(!S.transactions?.length)seedDemo();if(!S.accounts?.length)S.accounts=defAccs();setConn('offline');return;}
  try{
    await fetch(cfg.url+'/health');
    const[txR,buds,goals,state]=await Promise.all([api('GET','/api/transactions?limit=1000'),api('GET','/api/budgets'),api('GET','/api/goals'),api('GET','/api/state')]);
    S.transactions=(txR.data||[]).map(nTx);S.budgets=(buds||[]).map(nBud);S.goals=(goals||[]).map(nGoal);
    S.accounts=(state.accounts||[]).map(nAcc);
    if(!S.accounts.length)S.accounts=defAccs();
    custCats=(state.categories||[]).map(nCat);
    sl={lists:(state.shopping?.lists||[]).map(nShopList),items:(state.shopping?.items||[]).map(nShopItem)};
    if(!sl.lists.length)sl={lists:[{id:uid(),name:'Mercado',ico:'\u{1F6D2}'}],items:[]};
    slActiveList=state.settings?.active_list||sl.lists[0]?.id||null;
    applyRemoteSettings(state.settings||{});
    saveLocal();setConn('online');
    if(!(state.accounts||[]).length)saveRemoteState().catch(()=>{});
  }catch(e){
    console.warn('offline:',e.message);const c=loadLocal();
    S=c.transactions?.length?c:{transactions:[],budgets:[],goals:[],accounts:defAccs()};
    if(c.transactions?.length)toast('Offline - cache local','info');
    setConn('error');
  }
}
const ATYPES={checking:'Corrente',savings:'Poupança',credit:'Cartão',cash:'Dinheiro',investment:'Investimento'};
function getAccBal(id){
  const a=S.accounts.find(x=>x.id===id);if(!a)return 0;
  return a.balance+S.transactions.filter(t=>t.accountId===id&&!isFut(t.date)&&!t.paid).reduce((s,t)=>s+(t.type==='income'?t.amount:-t.amount),0);
}
function loadRates(){
  try{
    const r=JSON.parse(localStorage.getItem(RATES_KEY)||'{}');
    if(r.cdi)RATES.cdi=parseFloat(r.cdi);
    if(r.selic)RATES.selic=parseFloat(r.selic);
  }catch{}
}
function updateRates(){
  const cdi=parseFloat(document.getElementById('setCDI')?.value);
  const selic=parseFloat(document.getElementById('setSelic')?.value);
  if(cdi>0)RATES.cdi=cdi;
  if(selic>0)RATES.selic=selic;
  localStorage.setItem(RATES_KEY,JSON.stringify({cdi:RATES.cdi,selic:RATES.selic}));
  if(cfg.mode==='api')saveRemoteState().catch(e=>toast('Erro ao salvar taxas: '+e.message,'error'));
  toast(`Taxas atualizadas: CDI ${RATES.cdi}%  Selic ${RATES.selic}%`,'success');
  renderAccs();renderDash();
}
function calcEffectiveRate(type,val){
  switch(type){
    case 'cdi_pct':return RATES.cdi*val/100;
    case 'cdi_plus':return RATES.cdi+val;
    case 'fixed':return val;
    case 'selic':return RATES.selic*val/100;
    case 'poupanca':{
      const mensal=RATES.selic>8.5?0.5:(RATES.selic/100)*0.70/12*100;
      return (Math.pow(1+mensal/100,12)-1)*100+RATES.tr;
    }
    default:return val;
  }
}
function getAccRateInfo(a){
  if(a.type==='investment'&&a.yieldType&&a.yieldType!=='manual'){
    const aa=calcEffectiveRate(a.yieldType,parseFloat(a.yieldVal)||0);
    const base=a.calcBase||'du';
    const days=base==='du'?21:30;
    const daysYear=base==='du'?252:360;
    const monthly=(Math.pow(1+aa/100,days/daysYear)-1)*100;
    return {monthly,annual:aa,label:`${aa.toFixed(2)}% a.a.`};
  }
  const monthly=parseFloat(a.yieldRate)||0;
  return {monthly,annual:(Math.pow(1+monthly/100,12)-1)*100,label:`${monthly}% a.m.`};
}
function getAccYield(a,months=1){
  const bal=Math.max(getAccBal(a.id),0);
  const rate=(getAccRateInfo(a).monthly||0)/100;
  if(!bal||!rate)return 0;
  return bal*(Math.pow(1+rate,months)-1);
}
function getYieldSummary(){
  const accounts=S.accounts.map(a=>({account:a,month:getAccYield(a,1)})).filter(x=>x.month>0);
  const month=accounts.reduce((s,x)=>s+x.month,0);
  return {accounts,month,day:month/30,year:S.accounts.reduce((s,a)=>s+getAccYield(a,12),0)};
}
function openAccModal(id=null){
  accEditId=id;
  const a=id?normalizeAccount(S.accounts.find(x=>x.id===id)||{}):null;
  document.getElementById('accModalTitle').textContent=a?'Editar Conta':'Nova Conta';
  document.getElementById('accModalSub').textContent=a?'Ajuste saldo, tipo e rendimento':'Conta bancária, carteira ou investimento';
  document.getElementById('accNm').value=a?.name||'';
  document.getElementById('accIco').value=a?.icon||'';
  document.getElementById('accTyp').value=a?.type||'checking';
  document.getElementById('accBal').value=a?.balance??'';
  document.getElementById('accYield').value=a?.yieldRate??'';
  document.getElementById('accYieldType').value=a?.yieldType||'cdi_pct';
  document.getElementById('accYieldVal').value=a?.yieldVal||100;
  document.getElementById('accCalcBase').value=a?.calcBase||'du';
  document.getElementById('accStartDate').value=a?.startDate||'';
  document.getElementById('accNote').value=a?.note||'';
  toggleAccInvFields();
  updAccYieldPreview();
  document.getElementById('accModal').classList.add('open');
}
function saveAcc(){
  const name=document.getElementById('accNm').value.trim();
  const icon=document.getElementById('accIco').value.trim()||'🏦';
  const type=document.getElementById('accTyp').value;
  const balance=parseFloat(document.getElementById('accBal').value)||0;
  const yieldRate=parseFloat(document.getElementById('accYield').value)||0;
  const note=document.getElementById('accNote').value.trim();
  const yieldType=document.getElementById('accYieldType')?.value||'manual';
  const yieldVal=parseFloat(document.getElementById('accYieldVal')?.value)||100;
  const calcBase=document.getElementById('accCalcBase')?.value||'du';
  const startDate=document.getElementById('accStartDate')?.value||'';
  if(!name){toast('Informe o nome','error');return;}
  const rateInfo=type==='investment'?getAccRateInfo({type,yieldType,yieldVal,calcBase,yieldRate}):{monthly:yieldRate};
  const account={id:accEditId||uid(),name,icon,type,balance,yieldRate:type==='investment'?Number(rateInfo.monthly.toFixed(4)):yieldRate,yieldType,yieldVal,calcBase,startDate,note};
  if(accEditId){
    const i=S.accounts.findIndex(a=>a.id===accEditId);
    if(i>=0)S.accounts[i]=account;
  }else S.accounts.push(account);
  persistLocalOrRemote();closeM('accModal');renderAccs();renderDash();popAccSels();toast(accEditId?'Conta atualizada! ✓':'Conta criada! ✓','success');
  accEditId=null;
}
function toggleAccInvFields(){
  const type=document.getElementById('accTyp')?.value;
  const fields=document.getElementById('accInvFields');
  if(fields)fields.style.display=type==='investment'?'block':'none';
  updAccYieldPreview();
}
function updAccYieldPreview(){
  const el=document.getElementById('accYieldPreview');if(!el)return;
  const type=document.getElementById('accYieldType')?.value||'cdi_pct';
  const val=parseFloat(document.getElementById('accYieldVal')?.value)||0;
  const base=document.getElementById('accCalcBase')?.value||'du';
  const bal=parseFloat(document.getElementById('accBal')?.value)||0;
  const start=document.getElementById('accStartDate')?.value;
  const lbl=document.getElementById('accYieldLbl');
  const labels={cdi_pct:'% do CDI',cdi_plus:'% a.a. (spread)',fixed:'% a.a.',selic:'% da Selic',poupanca:''};
  if(lbl)lbl.textContent=labels[type]||'%';
  if(!val&&type!=='poupanca'){el.textContent='Digite o percentual para ver a projeo.';return;}
  if(!bal){el.textContent='Digite o saldo para ver a projeo.';return;}
  const aa=calcEffectiveRate(type,val);
  const diasAno=base==='du'?252:360;
  const daily=Math.pow(1+aa/100,1/diasAno)-1;
  const r1m=Math.pow(1+daily,base==='du'?21:30)-1;
  const r3m=Math.pow(1+daily,base==='du'?63:90)-1;
  const r12m=Math.pow(1+daily,diasAno)-1;
  let acum='';
  if(start){
    const diasPassados=Math.floor((Date.now()-new Date(start+'T12:00:00'))/86400000);
    const diasCalc=base==='du'?Math.round(diasPassados*252/365):diasPassados;
    const rAcum=Math.pow(1+daily,Math.max(0,diasCalc))-1;
    if(diasPassados>0)acum=`<br><strong>Rendimento acumulado:</strong> <span style="color:var(--ac)">${fmt(bal*rAcum)}</span> em ${diasPassados} dias`;
  }
  el.innerHTML=`<strong>Taxa efetiva:</strong> ${aa.toFixed(2)}% a.a.  ${base==='du'?'dias teis':'dias corridos'}<br><strong>Rendimento estimado:</strong><br>1 ms: <span style="color:var(--ac)">${fmt(bal*r1m)}</span>  3 meses: <span style="color:var(--ac)">${fmt(bal*r3m)}</span>  12 meses: <span style="color:var(--ac)">${fmt(bal*r12m)}</span>${acum}<br><span style="font-size:10px;opacity:.65">CDI ${RATES.cdi}% a.a.  Selic ${RATES.selic}% a.a.</span>`;
}
function delAcc(id){
  if(S.accounts.length<=1){toast('Mnimo 1 conta','error');return;}
  if(!confirm('Remover esta conta?'))return;
  S.accounts=S.accounts.filter(a=>a.id!==id);persistLocalOrRemote();renderAccs();popAccSels();
}
function popAccSels(){
  const opts=S.accounts.map(a=>`<option value="${a.id}">${a.icon} ${a.name}</option>`).join('');
  const aOpts='<option value="all">Todas</option>'+opts;
  ['txAcc'].forEach(id=>{const e=document.getElementById(id);if(e)e.innerHTML=opts;});
  ['trFrom','trTo'].forEach(id=>{const e=document.getElementById(id);if(e)e.innerHTML=opts;});
  const fa=document.getElementById('fAcc');if(fa)fa.innerHTML=aOpts;
}
async function doTransfer(){
  const from=document.getElementById('trFrom').value,to=document.getElementById('trTo').value;
  const amount=parseFloat(document.getElementById('trAmt').value);
  if(from===to){toast('Contas iguais','error');return;}
  if(!amount||amount<=0){toast('Valor inválido','error');return;}
  const fA=S.accounts.find(a=>a.id===from),tA=S.accounts.find(a=>a.id===to);
  const now=today();
  const mk=(type,desc,accId)=>({id:uid(),type,desc,amount,category:'Outros',date:now,note:'',accountId:accId,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:false,pending:false});
  const out=mk('expense',`Transferência → ${tA.name}`,from);
  const inc=mk('income',`Transferência ← ${fA.name}`,to);
  try{
    if(cfg.mode==='api'){
      const saved=await Promise.all([out,inc].map(t=>api('POST','/api/transactions',{type:t.type,description:t.desc,amount:t.amount,category:t.category,date:t.date,note:t.note,account_id:t.accountId,paid:false,pending:false})));
      S.transactions.unshift(...saved.map(nTx));
    }else{
      S.transactions.unshift(out,inc);
      saveLocal();
    }
    document.getElementById('trAmt').value='';renderAccs();renderDash();toast(`${fmt(amount)} transferido! OK`,'success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
function renderAccs(){
  const el=document.getElementById('accGrid');if(!el)return;
  const total=S.accounts.reduce((s,a)=>s+getAccBal(a.id),0);
  const invs=S.accounts.filter(a=>a.type==='investment');
  const invTotal=invs.reduce((s,a)=>s+Math.max(getAccBal(a.id),0),0);
  const y=getYieldSummary();
  const best=[...S.accounts].sort((a,b)=>getAccYield(b,1)-getAccYield(a,1))[0];
  const stats=document.getElementById('accStats');
  if(stats)stats.innerHTML=`
    <div class="insight-card"><div class="insight-k">Patrimônio</div><div class="insight-v money ${total>=0?'neu':'neg'}">${fmt(total)}</div></div>
    <div class="insight-card"><div class="insight-k">Investido</div><div class="insight-v" style="color:var(--ac2)">${fmt(invTotal)}</div><div class="cc">${invs.length} conta${invs.length!==1?'s':''}</div></div>
    <div class="insight-card"><div class="insight-k">Rendimento mensal</div><div class="insight-v" style="color:var(--ac)">${fmt(y.month)}</div><div class="cc">${fmt(y.year)}/ano estimado</div></div>
    <div class="insight-card"><div class="insight-k">Melhor conta</div><div class="insight-v" style="font-size:16px">${best?best.icon+' '+best.name:''}</div><div class="cc">${best?fmt(getAccYield(best,1))+'/mês':'sem dados'}</div></div>`;
  el.innerHTML=S.accounts.map(a=>{
    const bal=getAccBal(a.id);
    const y=getAccYield(a,1);
    const rate=getAccRateInfo(a);
    const yLbl=y>0?`<div class="yield-detail"><span class="yield-pill">+${fmt(y)}/mês</span><span>${rate.label}</span></div>`:`<div class="yield-detail">Sem rendimento configurado</div>`;
    return`<div class="bg-card"><div style="display:flex;justify-content:space-between;margin-bottom:10px"><div style="font-size:24px">${a.icon}</div><div style="display:flex;align-items:center;gap:4px"><span style="font-size:11px;color:var(--mt);background:var(--sf2);border:1px solid var(--bd);border-radius:5px;padding:2px 6px">${ATYPES[a.type]||a.type}</span><button class="ib" onclick="openAccModal('${a.id}')" title="Editar">✏️</button><button class="ib del" onclick="delAcc('${a.id}')" title="Remover">🗑️</button></div></div><div style="font-size:13px;font-weight:600;margin-bottom:3px">${a.name}</div><div class="money" style="font-size:20px;font-weight:700;color:${bal>=0?'var(--ac)':'var(--dan)'}">${fmt(bal)}</div>${yLbl}${a.note?`<div style="font-size:11px;color:var(--mt);margin-top:6px">${a.note}</div>`:''}</div>`;
  }).join('')+`<div class="bg-card" style="border-style:dashed;display:flex;align-items:center;justify-content:center;gap:7px;color:var(--mt);font-size:12px;cursor:pointer;" onclick="openAccModal()"><span style="font-size:20px">+</span> Nova Conta</div>`;
}
let qaOpen=false;
function toggleQA(){
  qaOpen=!qaOpen;
  document.getElementById('qaPanel').classList.toggle('open',qaOpen);
  const fab=document.getElementById('fabBtn');fab.classList.toggle('open',qaOpen);fab.textContent=qaOpen?'✕':'+';
  if(qaOpen){qaVal='';qaSelCat='';renderQACats();updQADisp();}
}
function qaType(t){
  qaTyp=t;
  document.getElementById('qaE').className='qa-tb'+(t==='expense'?' active expense':'');
  document.getElementById('qaI').className='qa-tb'+(t==='income'?' active income':'');
  updQADisp();
}
function qaSelC(n){qaSelCat=qaSelCat===n?'':n;renderQACats();}
function qaK(k){
  if(k==='del')qaVal=qaVal.slice(0,-1);
  else if(k==='.')qaVal.includes('.')||(qaVal+=k);
  else qaVal.length<8&&(qaVal+=k);
  updQADisp();
}
function updQADisp(){
  const v=parseFloat(qaVal)||0;
  const el=document.getElementById('qaAmt');
  el.textContent=v?fmt(v):'R$ 0';
  el.className='qa-amt '+(qaTyp==='expense'?'expense':'income');
}
async function qaSave(){
  const amount=parseFloat(qaVal)||0;
  if(!amount){toast('Digite um valor','error');return;}
  const cat=qaSelCat||'A classificar';
  const note=document.getElementById('qaNote').value.trim();
  const pending=!qaSelCat;
  const tx={id:uid(),type:qaTyp,desc:note||cat,amount,category:cat,date:today(),note:'',accountId:S.accounts[0]?.id||null,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:false,pending};
  if(cfg.mode==='api'){try{const r=await api('POST','/api/transactions',{type:qaTyp,description:tx.desc,amount,category:cat,date:tx.date,note:'',account_id:tx.accountId,paid:false,pending});S.transactions.unshift(nTx({...r,accountId:tx.accountId,pending}));}catch(e){toast('Erro: '+e.message,'error');return;}}
  else{S.transactions.unshift(tx);saveLocal();}
  toast(`${fmt(amount)} salvo${pending?'  classifique depois':''} ✓`,'success');
  qaVal='';qaSelCat='';document.getElementById('qaNote').value='';updQADisp();renderQACats();toggleQA();renderDash();
}
function openSrch(q=''){document.getElementById('srchOv').classList.add('open');const i=document.getElementById('srchInp');i.value=q;i.focus();if(q)doSrch(q);}
function closeSrch(){document.getElementById('srchOv').classList.remove('open');}
function doSrch(q){
  const el=document.getElementById('srchRes');
  if(!q||q.length<2){el.innerHTML='<div class="srch-empty">Digite para buscar...</div>';return;}
  const ql=q.toLowerCase();
  const txs=S.transactions.filter(t=>t.desc.toLowerCase().includes(ql)||t.category.toLowerCase().includes(ql)).slice(0,8);
  const goals=S.goals.filter(g=>g.name.toLowerCase().includes(ql)).slice(0,3);
  const buds=S.budgets.filter(b=>b.category.toLowerCase().includes(ql)).slice(0,3);
  if(!txs.length&&!goals.length&&!buds.length){el.innerHTML='<div class="srch-empty">Nenhum resultado.</div>';return;}
  let html='';
  if(txs.length){html+='<div class="srch-lbl">Transações</div>';html+=txs.map(t=>{const c=getCat(t.category);return`<div class="srch-row" onclick="closeSrch();showPage('transactions');setTimeout(()=>hlTx('${t.id}'),300)"><div style="width:30px;height:30px;border-radius:8px;background:${c.col}20;display:flex;align-items:center;justify-content:center;font-size:13px">${c.ico}</div><div style="flex:1"><div style="font-size:12px;font-weight:500">${t.desc}</div><div style="font-size:10px;color:var(--mt)">${fmtD(t.date)}  ${t.category}</div></div><div style="font-family:var(--font-money);font-size:12px;font-weight:700;color:${t.type==='income'?'var(--ac)':'var(--dan)'}">${t.type==='income'?'+':'-'}${fmt(t.amount)}</div></div>`;}).join('');}
  if(goals.length){html+='<div class="srch-lbl">Metas</div>';html+=goals.map(g=>`<div class="srch-row" onclick="closeSrch();showPage('goals')"><span style="font-size:18px">${g.icon}</span><div style="flex:1"><div style="font-size:12px;font-weight:500">${g.name}</div><div style="font-size:10px;color:var(--mt)">${fmt(g.current)} de ${fmt(g.target)}</div></div></div>`).join('');}
  if(buds.length){html+='<div class="srch-lbl">Orçamentos</div>';html+=buds.map(b=>{const c=getCat(b.category);return`<div class="srch-row" onclick="closeSrch();showPage('budget')"><span style="font-size:18px">${c.ico}</span><div style="flex:1"><div style="font-size:12px;font-weight:500">${b.category}</div><div style="font-size:10px;color:var(--mt)">Limite: ${fmt(b.limit)}</div></div></div>`;}).join('');}
  el.innerHTML=html;
}
document.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key==='k'){e.preventDefault();openSrch();}if(e.key==='Escape'){closeSrch();document.querySelectorAll('.ov.open').forEach(o=>o.classList.remove('open'));}});
function openModal(id=null,futDate=false){
  editId=id;const tx=id?S.transactions.find(t=>t.id===id):null;
  document.getElementById('mTit').textContent=tx?'Editar Transação':'Nova Transação';
  document.getElementById('mSub').textContent=tx?'Edite os dados':'Registre uma receita ou despesa';
  document.getElementById('txDesc').value=tx?.desc||'';
  document.getElementById('txAmt').value=tx?.amount||'';
  document.getElementById('txDt').value=tx?.date||(futDate?addM(today(),1):today());
  document.getElementById('txNote').value=tx?.note||'';
  popCatSels();popAccSels();
  if(tx?.category)document.getElementById('txCat').value=tx.category;
  if(tx?.accountId)document.getElementById('txAcc').value=tx.accountId;
  document.getElementById('instChk').checked=false;document.getElementById('instSec').style.display='none';
  document.getElementById('recChk').checked=false;document.getElementById('recSec').style.display='none';
  setTyp(tx?.type||'expense');document.getElementById('txModal').classList.add('open');
}
function dupTx(id){
  const tx=S.transactions.find(t=>t.id===id);if(!tx)return;
  editId=null;
  document.getElementById('mTit').textContent='Duplicar Transação';
  document.getElementById('txDesc').value=tx.desc;document.getElementById('txAmt').value=tx.amount;
  document.getElementById('txDt').value=today();document.getElementById('txNote').value=tx.note;
  popCatSels();popAccSels();document.getElementById('txCat').value=tx.category;
  if(tx.accountId)document.getElementById('txAcc').value=tx.accountId;
  document.getElementById('instChk').checked=false;document.getElementById('instSec').style.display='none';
  document.getElementById('recChk').checked=false;document.getElementById('recSec').style.display='none';
  setTyp(tx.type);document.getElementById('txModal').classList.add('open');
}
function togInst(){
  const on=document.getElementById('instChk').checked;
  document.getElementById('instSec').style.display=on?'block':'none';
  if(on){document.getElementById('recChk').checked=false;document.getElementById('recSec').style.display='none';document.getElementById('instSt').value=document.getElementById('txDt').value||today();}
}
function togRec(){
  const on=document.getElementById('recChk').checked;
  document.getElementById('recSec').style.display=on?'block':'none';
  if(on){document.getElementById('instChk').checked=false;document.getElementById('instSec').style.display='none';}
}
function updIPrev(){
  if(!document.getElementById('instChk').checked)return;
  const total=parseFloat(document.getElementById('txAmt').value)||0;
  const n=parseInt(document.getElementById('instN').value)||0;
  const start=document.getElementById('instSt').value;
  if(!total||!n||!start){document.getElementById('iPrev').textContent='Preencha valor e parcelas.';return;}
  const per=Math.round(total/n*100)/100;
  const dates=Array.from({length:n},(_,i)=>fmtD(addM(start,i)));
  document.getElementById('iPrev').innerHTML=`<strong>${fmt(per)}</strong>/mês  ${n} = ${fmt(total)}<br>${dates.slice(0,3).join(', ')}${n>3?` ... ${dates[n-1]}`:''}`;
}
async function saveTx(){
  const desc=document.getElementById('txDesc').value.trim();
  const amount=parseFloat(document.getElementById('txAmt').value);
  const category=document.getElementById('txCat').value;
  const date=document.getElementById('txDt').value;
  const note=document.getElementById('txNote').value.trim();
  const type=getTyp();const accountId=document.getElementById('txAcc').value;
  const isInst=document.getElementById('instChk').checked;
  const isRec=document.getElementById('recChk').checked;
  if(!desc||!amount||!date){toast('Preencha todos os campos','error');return;}
  if(amount<=0){toast('Valor deve ser positivo','error');return;}
  const btn=document.getElementById('saveTxBtn');btn.disabled=true;btn.textContent='Salvando...';
  try{
    if(isInst&&!editId){
      const n=parseInt(document.getElementById('instN').value)||1;
      const st=document.getElementById('instSt').value||date;
      if(n<2){toast('Mínimo 2 parcelas','error');return;}
      const per=Math.round(amount/n*100)/100,gid=uid();
      for(let i=0;i<n;i++){
        const d=addM(st,i);
        const tx={id:uid(),type,desc:`${desc} (${i+1}/${n})`,amount:per,category,date:d,note,accountId,installmentGroup:gid,installmentNum:i+1,installmentTotal:n,recurGroup:null,paid:false,pending:false};
        if(cfg.mode==='api'){const r=await api('POST','/api/transactions',{type,description:tx.desc,amount:per,category,date:d,note,account_id:accountId,paid:false,pending:false,installment_group:gid,installment_num:i+1,installment_total:n});S.transactions.unshift(nTx({...r,accountId}));}
        else S.transactions.unshift(tx);
      }
      if(cfg.mode==='local')saveLocal();toast(`${n} parcelas criadas! ✓`,'success');
    } else if(isRec&&!editId){
      const freq=document.getElementById('recFreq').value;
      const cnt=parseInt(document.getElementById('recN').value)||12;
      const gid=uid();const nD=(d,i)=>freq==='weekly'?addW(d,i):freq==='yearly'?addY(d,i):addM(d,i);
      for(let i=0;i<cnt;i++){
        const d=nD(date,i);
        const tx={id:uid(),type,desc,amount,category,date:d,note,accountId,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:gid,paid:false,pending:false};
        if(cfg.mode==='api'){const r=await api('POST','/api/transactions',{type,description:desc,amount,category,date:d,note,account_id:accountId,paid:false,pending:false,recur_group:gid});S.transactions.unshift(nTx({...r,accountId}));}
        else S.transactions.unshift(tx);
      }
      if(cfg.mode==='local')saveLocal();toast(`${cnt} lançamentos criados! ✓`,'success');
    } else {
      const oldTx=editId?S.transactions.find(t=>t.id===editId):null;
      const pl={type,description:desc,amount,category,date,note,account_id:accountId,paid:oldTx?.paid||false,pending:oldTx?.pending||false};
      if(cfg.mode==='api'){
        if(editId){const u=await api('PUT',`/api/transactions/${editId}`,pl);const i=S.transactions.findIndex(t=>t.id===editId);S.transactions[i]=nTx({...u,accountId});}
        else{const c=await api('POST','/api/transactions',pl);S.transactions.unshift(nTx({...c,accountId}));}
      } else {
        const tx={id:editId||uid(),type,desc,amount,category,date,note,accountId,installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:false,pending:false};
        if(editId){const i=S.transactions.findIndex(t=>t.id===editId);S.transactions[i]=tx;}else S.transactions.unshift(tx);
        saveLocal();
      }
      toast(editId?'Atualizado! ✓':'Salvo! ✓','success');
    }
    closeM('txModal');refreshAll();setTimeout(scheduleVencimentoNotifications,500);
  }catch(e){toast('Erro: '+e.message,'error');}
  finally{btn.disabled=false;btn.textContent='Salvar';}
}
async function delTx(id){
  if(!confirm('Remover?'))return;
  try{if(cfg.mode==='api')await api('DELETE',`/api/transactions/${id}`);S.transactions=S.transactions.filter(t=>t.id!==id);if(cfg.mode==='local')saveLocal();toast('Removida','error');refreshAll();}
  catch(e){toast('Erro: '+e.message,'error');}
}
async function delGrp(gid,field){
  if(!confirm('Remover TODOS do grupo?'))return;
  try{
    const toD=S.transactions.filter(t=>t[field]===gid);
    if(cfg.mode==='api')await Promise.all(toD.map(t=>api('DELETE',`/api/transactions/${t.id}`)));
    S.transactions=S.transactions.filter(t=>t[field]!==gid);
    if(cfg.mode==='local')saveLocal();toast(`${toD.length} removidos`,'error');refreshAll();
  }catch(e){toast('Erro: '+e.message,'error');}
}
async function markPaid(id){
  const tx=S.transactions.find(t=>t.id===id);if(!tx)return;
  const prev=tx.paid;tx.paid=!tx.paid;
  try{
    if(cfg.mode==='api')await api('PUT',`/api/transactions/${id}`,{type:tx.type,description:tx.desc,amount:tx.amount,category:tx.category,date:tx.date,note:tx.note||'',account_id:tx.accountId,paid:tx.paid,pending:tx.pending});
    else saveLocal();
  }catch(e){tx.paid=prev;toast('Erro: '+e.message,'error');return;}
  refreshAll();toast(tx.paid?'Marcado como pago OK':'Desmarcado','info');setTimeout(scheduleVencimentoNotifications,500);setTimeout(setupPersistentNotification,1000);
}
function hlTx(id){const el=document.getElementById('tx-'+id);if(el){el.scrollIntoView({behavior:'smooth',block:'center'});el.style.outline='2px solid var(--ac)';setTimeout(()=>el.style.outline='',2000);}}
function exportCSV(){
  // Exporta CSV com UTF-8 completo, emojis e caracteres especiais preservados
  const esc=v=>{
    const s=String(v==null?'':v);
    // Fora string limpa preservando emojis via normalize
    return '"'+s.normalize('NFC').replace(/"/g,'""')+'"';
  };
  const rows=[
    ['Data','Tipo','Descrição','Valor (R$)','Categoria','Conta','Parcela','Recorrente','Pago','Pendente','Observação']
  ];
  [...S.transactions]
    .sort((a,b)=>b.date.localeCompare(a.date))
    .forEach(t=>{
      const acc=S.accounts.find(x=>x.id===t.accountId);
      rows.push([
        t.date,
        t.type==='income'?'Receita':'Despesa',
        t.desc,
        Number(t.amount).toFixed(2).replace('.',','),
        t.category,
        acc?acc.name:'',
        t.installmentNum?`${t.installmentNum}/${t.installmentTotal}`:'',
        t.recurGroup?'Sim':'',
        t.paid?'Sim':'',
        t.pending?'Sim':'',
        t.note||''
      ]);
    });
  // Usa ; como separador (padro pt-BR Excel) e BOM UTF-8
  const sep=';';
  const csv=rows.map(r=>r.map(esc).join(sep)).join('\r\n');
  // TextEncoder garante UTF-8 correto com emojis
  const enc=new TextEncoder();
  const bom=new Uint8Array([0xEF,0xBB,0xBF]);
  const body=enc.encode(csv);
  const blob=new Blob([bom,body],{type:'text/csv;charset=utf-8'});
  const a=document.createElement('a');
  a.href=URL.createObjectURL(blob);
  a.download='finanza_'+today()+'.csv';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(()=>URL.revokeObjectURL(a.href),1000);
  toast('CSV exportado: '+(rows.length-1)+' lançamentos ✓','success');
}
function buildBackupData(){
  return {
    version:APP_VERSION,
    exported_at:new Date().toISOString(),
    app:'Finanza',
    user:cfg.userName||'local',
    stats:{
      transactions:S.transactions.length,
      budgets:S.budgets.length,
      goals:S.goals.length,
      accounts:S.accounts.length
    },
    transactions:S.transactions,
    budgets:S.budgets,
    goals:S.goals,
    accounts:S.accounts,
    categories:custCats,
    shopping:sl?.lists?.length?sl:(()=>{try{return JSON.parse(localStorage.getItem(SL_KEY)||'{}');}catch{return{lists:[],items:[]};}})(),
    settings:getAppSettings()
  };
}
function exportJson(){
  const backup=buildBackupData();
  const json=JSON.stringify(backup,null,2);
  const enc=new TextEncoder();
  const bytes=enc.encode(json);
  const blob=new Blob([bytes],{type:'application/json;charset=utf-8'});
  const a=document.createElement('a');
  a.href=URL.createObjectURL(blob);
  a.download='finanza_backup_'+today()+'.json';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(()=>URL.revokeObjectURL(a.href),1000);
  toast('Backup exportado: '+S.transactions.length+' transações OK','success');
}

function exportBackupFull(){
  exportJson();
  setTimeout(()=>exportCSV(),800);
}

async function migrateToOnline(){
  const url=prompt('URL do servidor:');if(!url)return;
  const key=prompt('Chave de acesso:');if(!key)return;
  try{
    const base=url.replace(/\/$/,'');
    const me=await fetch(base+'/api/me',{headers:{'x-api-key':key}});
    if(!me.ok)throw new Error('Chave inválida');const u=await me.json();
    toast('Migrando tudo...','info');
    const r=await fetch(base+'/api/import',{method:'PUT',headers:{'Content-Type':'application/json','x-api-key':key},body:JSON.stringify(buildBackupData())});
    if(!r.ok){const e=await r.json().catch(()=>({}));throw new Error(e.error||'Erro ao importar');}
    cfg={url:base,key,mode:'api',userName:u.name,userId:u.id};localStorage.setItem(CK,JSON.stringify(cfg));
    toast('Dados migrados para online OK','success');await initApp();
  }catch(e){toast('Erro: '+e.message,'error');}
}
function getRange(p){
  const t=today();
  const m={'7d-p':{from:offD(new Date(),-7),to:t},'1m-p':{from:offD(new Date(),-30),to:t},'3m-p':{from:offD(new Date(),-90),to:t},'6m-p':{from:offD(new Date(),-180),to:t},'all-p':{from:'2000-01-01',to:t},'1m-f':{from:t,to:offD(new Date(),30)},'3m-f':{from:t,to:offD(new Date(),90)},'all-f':{from:t,to:'2099-12-31'},'all':{from:'2000-01-01',to:'2099-12-31'}};
  return m[p]||{from:'2000-01-01',to:'2099-12-31'};
}
function getFutEnd(fp){const m={'7d':offD(new Date(),7),'15d':offD(new Date(),15),'1m':offD(new Date(),30),'3m':offD(new Date(),90),'6m':offD(new Date(),180),'all':'2099-12-31'};return m[fp]||offD(new Date(),30);}
document.querySelectorAll('[data-p]').forEach(b=>{b.addEventListener('click',()=>{document.querySelectorAll('[data-p]').forEach(x=>x.classList.remove('active'));b.classList.add('active');curTxP=b.dataset.p;renderTx();});});
document.querySelectorAll('[data-fp]').forEach(b=>{b.addEventListener('click',()=>{document.querySelectorAll('[data-fp]').forEach(x=>x.classList.remove('active'));b.classList.add('active');curFP=b.dataset.fp;renderFut();});});
function updM(){document.getElementById('curM').textContent=MO[curDt.getMonth()]+' '+curDt.getFullYear();}
function getMonthTx(d){return S.transactions.filter(t=>{const x=new Date(t.date+'T12:00:00');return x.getMonth()===d.getMonth()&&x.getFullYear()===d.getFullYear();});}
document.getElementById('prevM').onclick=()=>{curDt=new Date(curDt.getFullYear(),curDt.getMonth()-1,1);updM();renderDash();};
document.getElementById('nextM').onclick=()=>{curDt=new Date(curDt.getFullYear(),curDt.getMonth()+1,1);updM();renderDash();};
let pgHist=[];
function showPage(id){
  const prev=document.querySelector('.page.active')?.id?.replace('page-','');
  document.querySelectorAll('.page').forEach(p=>p.classList.remove('active'));
  document.getElementById('page-'+id)?.classList.add('active');
  document.querySelectorAll('.nav-item,.fn-item,[data-page]').forEach(n=>n.classList.toggle('active',n.dataset.page===id));
  if(prev&&prev!==id)pgHist.push(prev);
  window.scrollTo({top:0,behavior:'smooth'});
  if(id==='dashboard')renderDash();
  else if(id==='accounts'){renderAccs();popAccSels();}
  else if(id==='transactions')renderTx();
  else if(id==='future')renderFut();
  else if(id==='budget')renderBuds();
  else if(id==='goals')renderGoals();
  else if(id==='shopping'){loadSL();renderShopping();}
  else if(id==='settings')renderSet();
}
document.querySelectorAll('.nav-item,.fn-item,[data-page]').forEach(n=>{n.onclick=()=>showPage(n.dataset.page);});
function refreshAll(){renderDash();const id=document.querySelector('.page.active')?.id?.replace('page-','');if(id&&id!=='dashboard')showPage(id);}
function txHTML(tx){
  const cat=getCat(tx.category);const fut=isFut(tx.date);const dl=dDiff(tx.date);
  let cls='ti';if(tx.paid)cls+=' paid-tx';else if(tx.pending)cls+=' pnd-tx';else if(fut)cls+=' fut-tx';
  const amtCls=fut&&!tx.paid?'fut-c':tx.type==='income'?'income':'expense';
  const acc=S.accounts.find(a=>a.id===tx.accountId);
  let bdgs='';
  if(tx.paid)bdgs+='<span class="bdg bdg-ok">✓ pago</span>';
  else if(tx.pending)bdgs+='<span class="bdg bdg-p">❓ pendente</span>';
  if(tx.installmentNum)bdgs+=`<span class="bdg bdg-i">💳 ${tx.installmentNum}/${tx.installmentTotal}</span>`;
  if(tx.recurGroup)bdgs+='<span class="bdg bdg-r">🔄</span>';
  if(fut&&!tx.paid)bdgs+=`<span class="bdg bdg-f">🔮 ${dl>0?dl+'d':'hoje'}</span>`;
  const delBtn=tx.installmentGroup?`<button class="ib del" onclick="delGrp('${tx.installmentGroup}','installmentGroup')">🗑️</button>`:tx.recurGroup?`<button class="ib del" onclick="delGrp('${tx.recurGroup}','recurGroup')">🗑️</button>`:`<button class="ib del" onclick="delTx('${tx.id}')">🗑️</button>`;
  return`<div style="border-radius:14px;overflow:hidden;margin-bottom:0"><div class="ti" id="tx-${tx.id}"><div class="tico" style="background:${cat.col}20">${cat.ico}</div><div class="tinf"><div class="tnm">${tx.desc}</div><div class="tcat"><span class="bdg" style="background:${cat.col}20;color:${cat.col}">${tx.category}</span>${acc?`<span style="font-size:11px;color:var(--mt)">${acc.icon}</span>`:''}${bdgs}${tx.note?`<span style="color:var(--mt);font-size:9px">${tx.note}</span>`:''}</div></div><div class="tr"><div class="tam ${amtCls}">${tx.type==='income'?'+':'-'}${fmt(tx.amount)}</div><div class="tdt">${fmtD(tx.date)}</div></div><div class="tact">${fut||tx.pending?`<button class="ib ok" onclick="markPaid('${tx.id}')">${tx.paid?'↩':'✓'}</button>`:''}<button class="ib" onclick="openModal('${tx.id}')">✏️</button><button class="ib" onclick="dupTx('${tx.id}')">⧉</button>${delBtn}</div><button class="ib" onclick="openInlineEdit('${tx.id}')" title="Edição rápida" style="font-size:11px;color:var(--ac)">⚡</button></div></div><div class="ti-edit-wrap" id="ie-${tx.id}"></div></div>`;
}
function renderWeekly(){
  const el=document.getElementById('wsum'),an=document.getElementById('anom');
  const ws=offD(new Date(),-7);
  const wTx=S.transactions.filter(t=>t.date>=ws&&t.date<=today()&&!isFut(t.date));
  const wI=wTx.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
  const wE=wTx.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
  if(wTx.length){
    const sv=wI-wE;
    el.innerHTML=`<div class="wcard"><div style="font-size:26px">📅</div><div><div style="font-family:var(--font-money);font-size:15px;font-weight:700;margin-bottom:3px">Esta semana</div><div style="font-size:11px;color:var(--mt)">Gastou <strong>${fmt(wE)}</strong>  Recebeu <strong>${fmt(wI)}</strong>${sv>0?`  <span style="color:var(--ac)">Economizou ${fmt(sv)}</span>`:sv<0?`  <span style="color:var(--dan)">Dficit de ${fmt(Math.abs(sv))}</span>`:''}</div></div></div>`;
  } else el.innerHTML='';
  // Anomaly
  const cats={};getMonthTx(curDt).filter(t=>t.type==='expense'&&!isFut(t.date)).forEach(t=>{cats[t.category]=(cats[t.category]||0)+t.amount;});
  const anoms=[];
  for(const[cat,spent] of Object.entries(cats)){
    let tot=0,cnt=0;
    for(let i=1;i<=3;i++){const d=new Date(curDt.getFullYear(),curDt.getMonth()-i,1);const mt=getMonthTx(d).filter(t=>t.type==='expense'&&t.category===cat&&!isFut(t.date)).reduce((s,t)=>s+t.amount,0);if(mt>0){tot+=mt;cnt++;}}
    if(cnt>0){const avg=tot/cnt;const pct=Math.round(((spent-avg)/avg)*100);if(pct>=40)anoms.push({cat,spent,avg,pct});}
  }
  if(anoms.length){const a=anoms.sort((a,b)=>b.pct-a.pct)[0];const c=getCat(a.cat);an.innerHTML=`<div class="anom">${c.ico} <div><strong>${a.cat}</strong> está <strong>${a.pct}% acima</strong> da média (${fmt(a.avg)}/mês). Este mês: ${fmt(a.spent)}</div></div>`;}
  else an.innerHTML='';
}
// DASHBOARD
let flowChart,catChartObj;
function setChartMode(m){
  chartMode=m;
  renderDash();
}

// TRANSACTIONS
function setView(v){
  curView=v;localStorage.setItem(VK,v);
  if(cfg.mode==='api')saveRemoteState().catch(()=>{});
  ['N','C','Cal','Chart'].forEach(n=>document.getElementById('v'+n)?.classList.toggle('active',n.toLowerCase()===v));
  const chartWrap=document.getElementById('txChartWrap');
  if(chartWrap)chartWrap.style.display=v==='chart'?'block':'none';
  renderTx();
  if(v==='chart')renderTxCharts();
}
function renderTx(){
  const srch=document.getElementById('txSrch')?.value.toLowerCase()||'';
  const fTyp=document.getElementById('fTyp')?.value||'all';
  const fCat=document.getElementById('fCat')?.value||'all';
  const fAcc=document.getElementById('fAcc')?.value||'all';
  const fSort=document.getElementById('fSort')?.value||'dd';
  const fMin=parseFloat(document.getElementById('fMin')?.value)||0;
  const fMax=parseFloat(document.getElementById('fMax')?.value)||Infinity;
  const range=getRange(curTxP);
  const catSel=document.getElementById('fCat');
  if(catSel){const prev=catSel.value;catSel.innerHTML='<option value="all">Categoria</option>'+[...new Set(S.transactions.map(t=>t.category))].sort().map(c=>`<option value="${c}"${c===prev?' selected':''}>${getCat(c).ico} ${c}</option>`).join('');}
  renderCatFilterChips(fCat);
  let txs=[...S.transactions].filter(t=>t.date>=range.from&&t.date<=range.to);
  if(fTyp!=='all')txs=txs.filter(t=>t.type===fTyp);
  if(fCat!=='all')txs=txs.filter(t=>t.category===fCat);
  if(fAcc!=='all')txs=txs.filter(t=>t.accountId===fAcc);
  if(fMin>0)txs=txs.filter(t=>t.amount>=fMin);
  if(fMax<Infinity)txs=txs.filter(t=>t.amount<=fMax);
  if(srch)txs=txs.filter(t=>t.desc.toLowerCase().includes(srch)||t.category.toLowerCase().includes(srch)||t.note.toLowerCase().includes(srch));
  txs.sort((a,b)=>({dd:()=>b.date.localeCompare(a.date),da:()=>a.date.localeCompare(b.date),'ad':()=>b.amount-a.amount,'aa':()=>a.amount-b.amount}[fSort]||(() =>0))());
  const tI=txs.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
  const tE=txs.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
  const fE=txs.filter(t=>t.type==='expense'&&isFut(t.date)).reduce((s,t)=>s+t.amount,0);
  const pnd=txs.filter(t=>t.pending).length;
  document.getElementById('txSum').innerHTML=`<span style="color:var(--ac)">⬆ ${fmt(tI)}</span><span style="color:var(--dan)">⬇ ${fmt(tE)}</span>${fE>0?`<span style="color:var(--fut)">🔮 ${fmt(fE)}</span>`:''}${pnd>0?`<span style="color:var(--warn)">❓ ${pnd} pendente${pnd>1?'s':''}</span>`:''}<span style="color:var(--mt)">${txs.length} lançamento${txs.length!==1?'s':''}</span>`;
  renderTxInsights(txs);
  const el=document.getElementById('txView');
  if(curView==='cal'){el.innerHTML=renderCal(txs);}
  else{
    const renderLimit=curView==='c'?400:250;
    const visible=txs.slice(0,renderLimit);
    const more=txs.length>visible.length?`<div class="empty" style="padding:18px"><p>Mostrando ${visible.length} de ${txs.length}. Use busca ou filtros para refinar.</p></div>`:'';
    el.innerHTML=`<div class="tl${curView==='c'?' compact':''}">${visible.length?visible.map(txHTML).join('')+more:`<div class="empty"><span class="ei">🔍</span><p>Nenhuma transação no período.</p></div>`}</div>`;
  }
}
function setCatFilter(cat){
  const sel=document.getElementById('fCat');if(!sel)return;
  sel.value=cat||'all';
  renderTx();
}
function renderCatFilterChips(active='all'){
  const el=document.getElementById('catFilterChips');if(!el)return;
  const cats=[...new Set(S.transactions.map(t=>t.category).filter(Boolean))].sort();
  if(!cats.length){el.innerHTML='';return;}
  el.innerHTML=`<button class="cat-filter-chip ${active==='all'?'active':''}" onclick="setCatFilter('all')">Todas</button>`+
    cats.map(name=>{const c=getCat(name);return`<button class="cat-filter-chip ${active===name?'active':''}" style="--cat:${c.col}" onclick="setCatFilter('${name.replace(/'/g,"\\'")}')"><span>${c.ico}</span>${name}</button>`;}).join('');
}
function renderTxInsights(txs){
  const el=document.getElementById('txInsightCards');
  const heat=document.getElementById('txHeatmapWrap');
  if(!el)return;
  const expenses=txs.filter(t=>t.type==='expense'&&!t.paid);
  const incomes=txs.filter(t=>t.type==='income'&&!t.paid);
  const exp=expenses.reduce((s,t)=>s+t.amount,0);
  const inc=incomes.reduce((s,t)=>s+t.amount,0);
  const avg=expenses.length?exp/expenses.length:0;
  const top=expenses.reduce((m,t)=>!m||t.amount>m.amount?t:m,null);
  const cats={};expenses.forEach(t=>cats[t.category]=(cats[t.category]||0)+t.amount);
  const topCat=Object.entries(cats).sort((a,b)=>b[1]-a[1])[0];
  el.innerHTML=`
    <div class="insight-card"><div class="insight-k">Saldo no filtro</div><div class="insight-v" style="color:${inc-exp>=0?'var(--ac)':'var(--dan)'}">${fmt(inc-exp)}</div></div>
    <div class="insight-card"><div class="insight-k">Ticket mdio</div><div class="insight-v" style="color:var(--warn)">${fmt(avg)}</div><div class="cc">${expenses.length} despesas</div></div>
    <div class="insight-card"><div class="insight-k">Maior gasto</div><div class="insight-v" style="color:var(--dan)">${top?fmt(top.amount):''}</div><div class="cc">${top?top.desc:'sem dados'}</div></div>
    <div class="insight-card"><div class="insight-k">Categoria líder</div><div class="insight-v" style="font-size:16px">${topCat?getCat(topCat[0]).ico+' '+topCat[0]:''}</div><div class="cc">${topCat?fmt(topCat[1]):'sem dados'}</div></div>`;
  if(heat){
    const d=new Date(curDt.getFullYear(),curDt.getMonth(),1);
    const dim=new Date(d.getFullYear(),d.getMonth()+1,0).getDate();
    const dayTotals={};expenses.forEach(t=>dayTotals[t.date]=(dayTotals[t.date]||0)+t.amount);
    const vals=Object.values(dayTotals), max=vals.length?Math.max(...vals):1;
    const first=d.getDay();
    heat.style.display=curView==='chart'||curView==='cal'?'none':'block';
    heat.innerHTML=`<div class="bh"><div><div class="ct">Mapa de calor</div><div class="cs">Gastos por dia no mês selecionado</div></div><button class="btn btn-g btn-sm" onclick="setView('cal')">Calendário</button></div>
      <div class="heat-grid">${Array(first).fill('<div></div>').join('')}${Array.from({length:dim},(_,i)=>{
        const day=i+1,ds=`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
        const val=dayTotals[ds]||0,alpha=val?Math.max(.12,val/max):0;
        return `<div class="heat-cell" style="background:${val?`rgba(245,112,90,${alpha})`:'var(--sf2)'};color:${val?'#fff':'var(--mt)'}" title="${val?fmt(val):''}">${day}</div>`;
      }).join('')}</div>`;
  }
}
function renderCal(txs){
  const d=new Date(curDt.getFullYear(),curDt.getMonth(),1);
  const dim=new Date(d.getFullYear(),d.getMonth()+1,0).getDate();
  const fd=(d.getDay()+6)%7;
  const days=['Seg','Ter','Qua','Qui','Sex','Sb','Dom'];
  let html=`<div class="cal-grid">`;
  days.forEach(x=>html+=`<div class="cal-hdr">${x}</div>`);
  for(let i=0;i<fd;i++)html+=`<div class="cal-day empty"></div>`;
  const td=today();
  for(let day=1;day<=dim;day++){
    const ds=`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(day).padStart(2,'0')}`;
    const dTx=txs.filter(t=>t.date===ds);
    const dE=dTx.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
    const dI=dTx.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
    html+=`<div class="cal-day${ds===td?' today':''}${dTx.length?' has-tx':''}" onclick="filterCalDay('${ds}')">
      <div class="cal-num">${day}</div>
      ${dE>0?`<div class="cal-dot" style="background:var(--dan)"></div><div class="cal-amt" style="color:var(--dan)">-${(dE/1000).toFixed(1)}k</div>`:''}
      ${dI>0?`<div class="cal-dot" style="background:var(--ac)"></div>`:''}
    </div>`;
  }
  html+=`</div>`;return html;
}
function filterCalDay(ds){document.getElementById('txSrch').value=ds;setView('n');}
function debounce(fn,wait=160){let t;return(...args)=>{clearTimeout(t);t=setTimeout(()=>fn(...args),wait);};}
let renderTxTimer=null;
function renderTxDebounced(){clearTimeout(renderTxTimer);renderTxTimer=setTimeout(renderTx,140);}
document.getElementById('txSrch')?.addEventListener('input',renderTxDebounced);
document.getElementById('fTyp')?.addEventListener('change',renderTx);
document.getElementById('fCat')?.addEventListener('change',renderTx);
document.getElementById('fAcc')?.addEventListener('change',renderTx);
document.getElementById('fSort')?.addEventListener('change',renderTx);
// FUTURE
function renderFut(){
  const toDate=getFutEnd(curFP);const t=today();
  const fE=S.transactions.filter(x=>x.type==='expense'&&x.date>t&&x.date<=toDate&&!x.paid).sort((a,b)=>a.date.localeCompare(b.date));
  const fI=S.transactions.filter(x=>x.type==='income'&&x.date>t&&x.date<=toDate&&!x.paid).sort((a,b)=>a.date.localeCompare(b.date));
  const ovd=S.transactions.filter(x=>x.type==='expense'&&x.installmentGroup&&x.date<t&&!x.paid).sort((a,b)=>a.date.localeCompare(b.date));
  const tE=fE.reduce((s,x)=>s+x.amount,0);const tI=fI.reduce((s,x)=>s+x.amount,0);const tO=ovd.reduce((s,x)=>s+x.amount,0);const sal=tI-tE;
  document.getElementById('futDbt').textContent=fmt(tE);document.getElementById('futInc').textContent=fmt(tI);document.getElementById('futOvd').textContent=fmt(tO);
  const bEl=document.getElementById('futBal');bEl.textContent=fmt(sal);bEl.className='cv '+(sal>=0?'pos':'neg');
  document.getElementById('futBalLbl').textContent=sal>=0?'positivo ✓':`faltam ${fmt(Math.abs(sal))}`;
  if(fE.length){const n=fE[0],dl=dDiff(n.date);document.getElementById('futNxt').textContent=fmtD(n.date);document.getElementById('futNxtN').textContent=`${n.desc}  em ${dl}d`;}
  else{document.getElementById('futNxt').textContent='';document.getElementById('futNxtN').textContent='Nenhum';}
  const ovdS=document.getElementById('ovdSec');
  if(ovd.length){ovdS.style.display='block';document.getElementById('ovdList').innerHTML=ovd.map(x=>{const dl=Math.abs(dDiff(x.date));const c=getCat(x.category);return`<div class="ti ovd-tx"><div class="tico" style="background:rgba(245,112,90,.12)">${c.ico}</div><div class="tinf"><div class="tnm">${x.desc}</div><div class="tcat"><span class="bdg bdg-o">⚠️ ${dl}d atrasado</span></div></div><div class="tr"><div class="tam expense">-${fmt(x.amount)}</div><div class="tdt">${fmtD(x.date)}</div></div><div class="tact"><button class="ib ok" onclick="markPaid('${x.id}')">✓</button><button class="ib" onclick="openModal('${x.id}')">✏️</button><button class="ib del" onclick="delTx('${x.id}')">🗑️</button></div></div>`;}).join('');}
  else ovdS.style.display='none';
  const incS=document.getElementById('futIncSec');
  if(fI.length){incS.style.display='block';document.getElementById('futIncList').innerHTML=fI.map(x=>{const dl=dDiff(x.date);const c=getCat(x.category);return`<div class="ti" style="border-left:3px solid var(--ac);background:rgba(200,245,90,.03)"><div class="tico" style="background:rgba(200,245,90,.1)">${c.ico}</div><div class="tinf"><div class="tnm">${x.desc}</div><div class="tcat"><span class="bdg bdg-f" style="color:var(--ac)">💰 em ${dl}d</span><span class="bdg" style="background:${c.col}20;color:${c.col}">${x.category}</span></div></div><div class="tr"><div class="tam income">+${fmt(x.amount)}</div><div class="tdt">${fmtD(x.date)}</div></div><div class="tact"><button class="ib ok" onclick="markPaid('${x.id}')">✓</button><button class="ib" onclick="openModal('${x.id}')">✏️</button><button class="ib del" onclick="delTx('${x.id}')">🗑️</button></div></div>`;}).join('');}
  else incS.style.display='none';
  document.getElementById('futExpList').innerHTML=fE.length?fE.map(x=>{const dl=dDiff(x.date);const c=getCat(x.category);const urg=dl<=3?'var(--dan)':dl<=7?'var(--warn)':'var(--fut)';return`<div class="ti fut-tx"><div class="tico" style="background:rgba(167,139,250,.1)">${c.ico}</div><div class="tinf"><div class="tnm">${x.desc}</div><div class="tcat"><span class="bdg" style="background:rgba(167,139,250,.12);color:${urg}">🔮 em ${dl}d</span><span class="bdg" style="background:${c.col}20;color:${c.col}">${x.category}</span>${x.installmentNum?`<span class="bdg bdg-i">💳 ${x.installmentNum}/${x.installmentTotal}</span>`:''}</div></div><div class="tr"><div class="tam fut-c">-${fmt(x.amount)}</div><div class="tdt">${fmtD(x.date)}</div></div><div class="tact"><button class="ib ok" onclick="markPaid('${x.id}')">✓</button><button class="ib" onclick="openModal('${x.id}')">✏️</button><button class="ib" onclick="dupTx('${x.id}')">⧉</button><button class="ib del" onclick="${x.installmentGroup?`delGrp('${x.installmentGroup}','installmentGroup')`:x.recurGroup?`delGrp('${x.recurGroup}','recurGroup')`:`delTx('${x.id}')`}">🗑️</button></div></div>`;}).join(''):`<div class="empty"><span class="ei">🔮</span><p>Nenhum gasto futuro no período.</p></div>`;
}
// BUDGETS
function openBudModal(){popCatSels();document.getElementById('budLim').value='';document.getElementById('budModal').classList.add('open');}
async function saveBud(){
  const cat=document.getElementById('budCat').value;const lim=parseFloat(document.getElementById('budLim').value);
  if(!lim||lim<=0){toast('Valor invlido','error');return;}
  try{
    if(cfg.mode==='api'){const r=await api('POST','/api/budgets',{category:cat,limit:lim});const i=S.budgets.findIndex(b=>b.category===cat);if(i>=0)S.budgets[i]=nBud(r);else S.budgets.push(nBud(r));}
    else{const i=S.budgets.findIndex(b=>b.category===cat);if(i>=0)S.budgets[i].limit=lim;else S.budgets.push({id:uid(),category:cat,limit:lim});saveLocal();}
    closeM('budModal');renderBuds();toast('Oramento salvo! ✓','success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
async function delBud(id){
  try{if(cfg.mode==='api')await api('DELETE',`/api/budgets/${id}`);S.budgets=S.budgets.filter(b=>b.id!==id);if(cfg.mode==='local')saveLocal();renderBuds();}
  catch(e){toast('Erro: '+e.message,'error');}
}
function renderBuds(){
  const now=new Date();let tL=0,tS=0;
  const cards=S.budgets.map(b=>{
    const spent=S.budgets&&S.transactions.filter(t=>t.type==='expense'&&t.category===b.category&&!isFut(t.date)&&!t.paid&&(()=>{const d=new Date(t.date+'T12:00:00');return d.getMonth()===now.getMonth()&&d.getFullYear()===now.getFullYear();})()).reduce((s,t)=>s+t.amount,0);
    const pct=Math.min((spent/b.limit)*100,100);const col=pct>=100?'var(--dan)':pct>=80?'var(--warn)':'var(--ac)';const rem=b.limit-spent;tL+=b.limit;tS+=spent;const cat=getCat(b.category);
    return`<div class="bg-card"><div style="display:flex;justify-content:space-between;margin-bottom:9px"><div><div style="font-size:18px">${cat.ico}</div><div style="font-size:11px;font-weight:600;margin-top:2px">${b.category}</div></div><button class="ib del" onclick="delBud('${b.id}')">🗑️</button></div><div style="display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:3px"><div style="font-family:var(--font-money);font-size:18px;font-weight:700;color:${col}">${fmt(spent)}</div><div style="font-size:10px;color:var(--mt)">de ${fmt(b.limit)}</div></div><div class="prg"><div class="pf" style="width:${pct}%;background:${col}"></div></div><div style="font-size:12px;color:${rem<0?'var(--dan)':'var(--mt)'};margin-top:5px">${rem>=0?`Restam ${fmt(rem)}`:`Excedido em ${fmt(Math.abs(rem))}`}</div></div>`;
  });
  document.getElementById('budTot').textContent=fmt(tL);document.getElementById('budSpent').textContent=fmt(tS);document.getElementById('budAvail').textContent=fmt(tL-tS);
  document.getElementById('budGrid').innerHTML=cards.length?cards.join(''):`<div class="empty" style="grid-column:1/-1"><span class="ei">🎯</span><p>Nenhum orçamento. Crie um!</p></div>`;
}
// GOALS
function openGoalModal(){['gNm','gIco','gDesc','gMon','gTgt','gCur'].forEach(id=>document.getElementById(id).value='');document.getElementById('goalModal').classList.add('open');}
async function saveGoal(){
  const name=document.getElementById('gNm').value.trim();const icon=document.getElementById('gIco').value.trim()||'🎯';
  const target=parseFloat(document.getElementById('gTgt').value);const current=parseFloat(document.getElementById('gCur').value)||0;
  const deadline=document.getElementById('gDl').value;const desc=document.getElementById('gDesc').value.trim();const monthly=parseFloat(document.getElementById('gMon').value)||0;
  if(!name||!target||!deadline){toast('Preencha os campos obrigatrios','error');return;}
  try{
    if(cfg.mode==='api'){const r=await api('POST','/api/goals',{name,icon,target,current,deadline,description:desc,monthly});S.goals.push(nGoal(r));}
    else{S.goals.push({id:uid(),name,icon,target,current,deadline,desc,monthly});saveLocal();}
    closeM('goalModal');renderGoals();toast('Meta criada! ✓','success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
async function delGoal(id){
  try{if(cfg.mode==='api')await api('DELETE',`/api/goals/${id}`);S.goals=S.goals.filter(g=>g.id!==id);if(cfg.mode==='local')saveLocal();renderGoals();}
  catch(e){toast('Erro: '+e.message,'error');}
}
async function addGoalAmt(id){
  const v=prompt('Quanto adicionar? (R$)');if(v===null)return;
  const n=parseFloat(v.replace(',','.'));if(isNaN(n)||n<=0){toast('Valor invlido','error');return;}
  try{
    if(cfg.mode==='api'){const u=await api('PATCH',`/api/goals/${id}/add`,{amount:n});const i=S.goals.findIndex(g=>g.id===id);S.goals[i]=nGoal(u);}
    else{const g=S.goals.find(g=>g.id===id);g.current=Math.min(g.current+n,g.target);saveLocal();}
    renderGoals();toast(`${fmt(n)} adicionado! ✓`,'success');
  }catch(e){toast('Erro: '+e.message,'error');}
}
function renderGoals(){
  const el=document.getElementById('goalGrid');if(!el)return;
  const stats=document.getElementById('goalStats');
  if(stats){
    const target=S.goals.reduce((s,g)=>s+g.target,0);
    const current=S.goals.reduce((s,g)=>s+g.current,0);
    const done=S.goals.filter(g=>g.current>=g.target).length;
    const monthly=S.goals.reduce((s,g)=>s+(g.monthly||0),0);
    stats.innerHTML=`
      <div class="insight-card"><div class="insight-k">Guardado</div><div class="insight-v" style="color:var(--ac)">${fmt(current)}</div><div class="cc">de ${fmt(target)}</div></div>
      <div class="insight-card"><div class="insight-k">Progresso geral</div><div class="insight-v" style="color:var(--ac2)">${target?Math.round(current/target*100):0}%</div></div>
      <div class="insight-card"><div class="insight-k">Metas concludas</div><div class="insight-v">${done}/${S.goals.length}</div></div>
      <div class="insight-card"><div class="insight-k">Aporte planejado</div><div class="insight-v" style="color:var(--warn)">${fmt(monthly)}</div><div class="cc">por mês</div></div>`;
  }
  if(!S.goals.length){el.innerHTML=`<div class="empty" style="grid-column:1/-1"><span class="ei">🏆</span><p>Nenhuma meta. Crie uma!</p></div>`;return;}
  el.innerHTML=S.goals.map(g=>{
    const pct=Math.min((g.current/g.target)*100,100);const col=pct>=100?'var(--ac)':pct>=60?'var(--ac2)':'var(--warn)';
    const dl=Math.ceil((new Date(g.deadline+'T12:00:00')-new Date())/864e5);const rem=g.target-g.current;
    const proj=g.monthly>0&&pct<100?`<div style="font-size:12px;color:var(--mt);margin-top:5px">📅 ~${Math.ceil(rem/g.monthly)} meses com ${fmt(g.monthly)}/mês</div>`:'';
    return`<div class="bg-card"><span style="font-size:24px;margin-bottom:9px;display:block">${g.icon}</span><div style="font-family:var(--font-money);font-size:13px;font-weight:800;margin-bottom:2px">${g.name}</div><div style="font-size:10px;color:var(--mt);margin-bottom:10px">${g.desc}</div><div style="display:flex;justify-content:space-between;margin-bottom:5px"><div style="font-family:var(--font-money);font-size:20px;font-weight:700;color:var(--ac)">${fmt(g.current)}</div><div style="font-size:10px;color:var(--mt);align-self:flex-end">de ${fmt(g.target)}</div></div><div class="prg"><div class="pf" style="width:${pct}%;background:${col}"></div></div><div style="display:flex;justify-content:space-between;margin-top:5px"><div style="font-size:11px;color:var(--mt)">📅 ${dl>0?dl+'d':'Encerrado'}</div><span style="font-size:11px;font-weight:600;color:${col}">${Math.round(pct)}%</span></div>${proj}<div style="display:flex;gap:4px;margin-top:9px"><button class="btn btn-g btn-sm" style="flex:1" onclick="addGoalAmt('${g.id}')">+ Adicionar</button><button class="ib del" onclick="delGoal('${g.id}')">🗑️</button></div></div>`;
  }).join('');
}
// SETTINGS
function renderSet(){
  const isDark=document.documentElement.dataset.theme==='dark';
  renderWidgetToggles();
  const cdi=document.getElementById('setCDI');if(cdi)cdi.value=RATES.cdi;
  const selic=document.getElementById('setSelic');if(selic)selic.value=RATES.selic;
  const tog=document.getElementById('thmTog');if(tog)tog.checked=isDark;
  document.getElementById('setUsr').textContent=cfg.userName||'';
  document.getElementById('setMod').textContent=cfg.mode==='api'?'Online (API + PostgreSQL)':'Local (neste dispositivo)';
  document.getElementById('setUrl').textContent=cfg.url||'Modo local';
  document.getElementById('setTxCt').textContent=S.transactions.length;
  const loc=cfg.mode==='local';
  const jRow=document.getElementById('setJsonRow');if(jRow)jRow.style.display=loc?'flex':'none';
  const mRow=document.getElementById('setMigRow');if(mRow)mRow.style.display=loc?'flex':'none';
  renderCatChips();
  const pJ=document.getElementById('popJsonExp');if(pJ)pJ.style.display=loc?'':'none';
  const pM=document.getElementById('popMig');if(pM)pM.style.display=loc?'':'none';
  renderDiag();
}
function getDiagText(){
  const syncPending=Array.isArray(syncQ)?syncQ.length:0;
  return [
    `Finanza ${APP_VERSION}`,
    `Modo: ${cfg.mode==='api'?'Online':'Local'}`,
    `Usuário: ${cfg.userName||'local'}`,
    `Servidor: ${cfg.url||'modo local'}`,
    `Transações: ${S.transactions.length}`,
    `Contas: ${S.accounts.length}`,
    `Metas: ${S.goals.length}`,
    `Orçamentos: ${S.budgets.length}`,
    `Sync pendente: ${syncPending}`,
    `Tema: ${document.documentElement.dataset.theme||'dark'}`
  ].join('\n');
}
function renderDiag(){
  const syncPending=Array.isArray(syncQ)?syncQ.length:0;
  const v=document.getElementById('diagVersion');if(v)v.textContent=APP_VERSION;
  const m=document.getElementById('diagMode');if(m)m.textContent=cfg.mode==='api'?`Online - ${cfg.url||'sem URL'}`:'Local neste dispositivo';
  const s=document.getElementById('diagSync');if(s)s.textContent=syncPending?`${syncPending} item(ns) pendente(s)`:'Sem pendencias';
  const d=document.getElementById('diagData');if(d)d.textContent=`${S.transactions.length} transações, ${S.accounts.length} contas, ${S.goals.length} metas`;
}
async function copyDiag(){
  const txt=getDiagText();
  try{await navigator.clipboard.writeText(txt);toast('Diagnostico copiado','success');}
  catch{toast(txt.replace(/\n/g,' | '),'info');}
}
// POPOVER
function togglePop(){const p=document.getElementById('acctPop');const c=document.getElementById('popChev');p.classList.toggle('open');c.textContent=p.classList.contains('open')?'▼':'▲';}
function closePop(){document.getElementById('acctPop').classList.remove('open');document.getElementById('popChev').textContent='▲';}
document.addEventListener('click',e=>{const btn=document.getElementById('acctBtn');const pop=document.getElementById('acctPop');if(pop&&!pop.contains(e.target)&&btn&&!btn.contains(e.target))closePop();});
// HELPERS
function setTyp(t){document.getElementById('tExp').className='ttb'+(t==='expense'?' active expense':'');document.getElementById('tInc').className='ttb'+(t==='income'?' active income':'');document.getElementById('tExp').dataset.t=t==='expense'?'1':'';document.getElementById('tInc').dataset.t=t==='income'?'1':'';}
function getTyp(){return document.getElementById('tExp').dataset.t?'expense':'income';}
function closeM(id){document.getElementById(id).classList.remove('open');}
document.querySelectorAll('.ov').forEach(o=>{let md=null;o.addEventListener('mousedown',e=>{md=e.target;});o.addEventListener('mouseup',e=>{if(e.target===o&&md===o)o.classList.remove('open');md=null;});let td=null;o.addEventListener('touchstart',e=>{td=e.target;},{passive:true});o.addEventListener('touchend',e=>{if(td===o&&e.target===o)o.classList.remove('open');td=null;});});
function toast(msg,type='success'){const el=document.createElement('div');el.className=`toast ${type}`;el.innerHTML=({success:'✅',error:'❌',info:'ℹ️'}[type]||'')+' '+msg;document.getElementById('twrap').appendChild(el);setTimeout(()=>el.remove(),3500);}
function clearCache(){if(!confirm('Limpar cache local?'))return;localStorage.removeItem(LK);toast('Cache limpo. Recarregando...','info');setTimeout(()=>location.reload(),1000);}
// RELOAD
async function doReload(){
  ['rlBtn','rlBtnS'].forEach(id=>{const b=document.getElementById(id);if(b){b.classList.add('spin');b.disabled=true;}});
  try{await loadAll();popCatSels();popAccSels();refreshAll();toast('Dados atualizados! ✓','success');}
  catch(e){toast('Erro: '+e.message,'error');}
  finally{['rlBtn','rlBtnS'].forEach(id=>{const b=document.getElementById(id);if(b){b.classList.remove('spin');b.disabled=false;}});}
}
// PTR
(()=>{
  let sy=0,pulling=false,triggered=false;const th=80;
  const ind=document.getElementById('ptr'),arr=document.getElementById('ptrAr'),txt=document.getElementById('ptrTxt');
  document.addEventListener('touchstart',e=>{if(window.scrollY===0)sy=e.touches[0].clientY;},{passive:true});
  document.addEventListener('touchmove',e=>{if(!sy)return;const dy=e.touches[0].clientY-sy;if(dy>10&&window.scrollY===0){pulling=true;ind.classList.add('show');if(dy>th){triggered=true;arr.classList.add('ready');txt.textContent='Solte para atualizar';}else{triggered=false;arr.classList.remove('ready');txt.textContent='Puxe para atualizar';}}},{passive:true});
  document.addEventListener('touchend',()=>{if(triggered){ind.classList.add('loading');txt.textContent='Atualizando...';doReload().then(()=>setTimeout(()=>{ind.classList.remove('show','loading');arr.classList.remove('ready');txt.textContent='Puxe para atualizar';},400));}else if(pulling)ind.classList.remove('show');sy=0;pulling=false;triggered=false;});
})();
// BACK
(()=>{
  let once=false,timer=null;const bt=document.getElementById('btst');
  history.pushState({fz:true},'','');
  window.addEventListener('popstate',()=>{
    if(pgHist.length>0){showPage(pgHist.pop());history.pushState({fz:true},'','');return;}
    const active=document.querySelector('.page.active')?.id?.replace('page-','');
    if(active&&active!=='dashboard'){showPage('dashboard');history.pushState({fz:true},'','');return;}
    if(once){if(window.Capacitor?.Plugins?.App)window.Capacitor.Plugins.App.exitApp();return;}
    once=true;bt.classList.add('show');history.pushState({fz:true},'','');
    if(timer)clearTimeout(timer);timer=setTimeout(()=>{once=false;bt.classList.remove('show');},2500);
  });
})();
// DEMO
function seedDemo(){
  const y=new Date().getFullYear(),m=String(new Date().getMonth()+1).padStart(2,'0');
  const pm=String(new Date().getMonth()).padStart(2,'0'),py=new Date().getMonth()===0?y-1:y;
  const a1=uid(),a2=uid(),gid=uid(),rid=uid();
  S.accounts=[{id:a1,name:'Principal',icon:'\u{1F3E6}',type:'checking',balance:1500,yieldRate:0,note:''},{id:a2,name:'Poupança',icon:'\u{1F416}',type:'savings',balance:8000,yieldRate:0.55,note:'Rendimento estimado'}];
  const mk=(type,desc,amount,category,date,aId=a1,extra={})=>({id:uid(),type,desc,amount,category,date,note:'',accountId:aId,...{installmentGroup:null,installmentNum:null,installmentTotal:null,recurGroup:null,paid:false,pending:false},...extra});
  S.transactions=[
    mk('income','Salário',5500,'Salário',`${y}-${m}-05`),
    mk('expense','Aluguel',1400,'Moradia',`${y}-${m}-07`),
    mk('expense','Supermercado',480,'Alimentação',`${y}-${m}-10`),
    mk('expense','Uber',95,'Transporte',`${y}-${m}-12`),
    mk('income','Freelance',1200,'Freelance',`${y}-${m}-15`),
    mk('expense','Netflix',55,'Assinaturas',`${y}-${m}-16`),
    mk('expense','Compra rápida',37.5,'A classificar',`${y}-${m}-17`,a1,{pending:true}),
    mk('expense','iPhone (1/4)',750,'Tecnologia',`${y}-${m}-20`,a1,{installmentGroup:gid,installmentNum:1,installmentTotal:4}),
    mk('expense','iPhone (2/4)',750,'Tecnologia',addM(`${y}-${m}-20`,1),a1,{installmentGroup:gid,installmentNum:2,installmentTotal:4}),
    mk('expense','iPhone (3/4)',750,'Tecnologia',addM(`${y}-${m}-20`,2),a1,{installmentGroup:gid,installmentNum:3,installmentTotal:4}),
    mk('expense','iPhone (4/4)',750,'Tecnologia',addM(`${y}-${m}-20`,3),a1,{installmentGroup:gid,installmentNum:4,installmentTotal:4}),
    mk('expense','Aluguel',1400,'Moradia',addM(`${y}-${m}-07`,1),a1,{recurGroup:rid}),
    mk('expense','Aluguel',1400,'Moradia',addM(`${y}-${m}-07`,2),a1,{recurGroup:rid}),
    mk('income','Salário',5500,'Salário',`${py}-${pm}-05`),
    mk('expense','Aluguel',1400,'Moradia',`${py}-${pm}-07`),
    mk('expense','Supermercado',520,'Alimentação',`${py}-${pm}-12`),
    mk('expense','Lazer',180,'Lazer',`${py}-${pm}-20`),
  ];
  S.budgets=[{id:uid(),category:'Alimentação',limit:600},{id:uid(),category:'Moradia',limit:1500},{id:uid(),category:'Transporte',limit:200},{id:uid(),category:'Lazer',limit:300},{id:uid(),category:'Assinaturas',limit:150},{id:uid(),category:'Tecnologia',limit:400}];
  S.goals=[{id:uid(),name:'Viagem Europa',icon:'\u2708\uFE0F',target:12000,current:4500,deadline:`${y+1}-06-01`,desc:'Lisboa e Berlim',monthly:800},{id:uid(),name:'Reserva Emergência',icon:'\u{1F6E1}\uFE0F',target:15000,current:8200,deadline:`${y}-12-31`,desc:'6 meses de despesas',monthly:500},{id:uid(),name:'Notebook Novo',icon:'\u{1F4BB}',target:6000,current:2100,deadline:`${y+1}-03-01`,desc:'Trabalho e estudos',monthly:300}];
  saveLocal();
}
// INIT
async function initApp(){
  loadWidgetPrefs();
  loadRates();
  await loadAll();if(cfg.mode==='local')loadCC();popCatSels();popAccSels();updM();renderDash();
  const name=cfg.userName||'Eu';
  document.getElementById('uName').textContent=name;
  applyAvatar();
  const sv=localStorage.getItem(VK)||'n';setView(sv);
  loadSyncQ();
  updSyncBadge();
  checkAutoBackup();
  initDeepLink();
  setTimeout(initNotifications, 3000);
  setTimeout(registerNotifActions, 1000);
  setTimeout(setupPersistentNotification, 5000);
}

// Detect Capacitor and tune the mobile shell.
(function detectCapacitor() {
  const isCapacitor = !!window.Capacitor;
  const isAndroid = /Android/i.test(navigator.userAgent || '');
  document.body.classList.toggle('is-capacitor', isCapacitor);
  document.body.classList.toggle('is-android', isAndroid);
  if (isCapacitor || isAndroid) {
    document.body.classList.add('no-blur', 'mobile-shell');
    const statusH = window.Capacitor?.Plugins?.StatusBar ? 24 : 0;
    document.documentElement.style.setProperty('--status-bar', statusH + 'px');
  }
  const syncViewport = () => {
    document.documentElement.style.setProperty('--app-vh', `${window.innerHeight * 0.01}px`);
  };
  syncViewport();
  window.addEventListener('resize', syncViewport, { passive: true });
  window.addEventListener('orientationchange', () => setTimeout(syncViewport, 250), { passive: true });
})();


// ════════════════════════════════════════════════════════════
// SYNC QUEUE  operaes offline aguardando
// ════════════════════════════════════════════════════════════
let syncQ=[];
function loadSyncQ(){try{syncQ=JSON.parse(localStorage.getItem('fz_syncq')||'[]');}catch{syncQ=[];}}
function saveSyncQ(){localStorage.setItem('fz_syncq',JSON.stringify(syncQ));}
function addToQueue(op){
  syncQ.push({...op,ts:Date.now()});
  saveSyncQ();
  updSyncBadge();
}
function updSyncBadge(){
  const b=document.getElementById('syncBadge');
  const ct=document.getElementById('syncCount');
  if(!b)return;
  if(syncQ.length>0){b.classList.add('visible');if(ct)ct.textContent=syncQ.length;}
  else b.classList.remove('visible');
}
async function syncQueue(){
  if(!syncQ.length){toast('Nada para sincronizar','info');return;}
  if(cfg.mode!=='api'){toast('Conecte ao servidor primeiro','error');return;}
  let ok=0,fail=0;
  for(const op of [...syncQ]){
    try{
      if(op.method==='POST')await api('POST',op.path,op.body);
      else if(op.method==='DELETE')await api('DELETE',op.path);
      else if(op.method==='PUT')await api('PUT',op.path,op.body);
      syncQ=syncQ.filter(q=>q.ts!==op.ts);
      ok++;
    }catch{fail++;}
  }
  saveSyncQ();updSyncBadge();
  toast(`Sincronizado: ${ok} OK${fail?', '+fail+' falha(s)':''}`,fail?'error':'success');
}

// ════════════════════════════════════════════════════════════
// CONNECTION BAR  indicador visual offline/online
// ════════════════════════════════════════════════════════════
let _lastConnState='';
function showConnBar(state,msg,duration=3000){
  if(state===_lastConnState&&state==='online')return;
  _lastConnState=state;
  const b=document.getElementById('connBar');
  if(!b)return;
  const icons={online:'✅',offline:'📴',error:'⚠️',syncing:'🔄'};
  b.className='conn-bar '+state+' show';
  b.innerHTML=`<span>${icons[state]||''}</span><span>${msg}</span>`;
  if(duration>0)setTimeout(()=>b.classList.remove('show'),duration);
}


// ════════════════════════════════════════════════════════════
// AUTO BACKUP
// ════════════════════════════════════════════════════════════
const BK_KEY='fz_last_backup';
const BK_INTERVAL_DAYS=3;
function checkAutoBackup(){
  const last=parseInt(localStorage.getItem(BK_KEY)||'0');
  const daysSince=(Date.now()-last)/(1000*60*60*24);
  if(daysSince>=BK_INTERVAL_DAYS||!last){
    // Silencioso: s avisa se > 3 dias
    if(last>0){
      const el=document.getElementById('backupStatusTxt');
      if(el)el.textContent='Backup automtico...';
    }
    setTimeout(()=>autoBackupNow(),2000);
  }
  updBackupStatus();
}
function autoBackupNow(){
  exportJson();
  localStorage.setItem(BK_KEY,Date.now().toString());
  updBackupStatus();
  if(parseInt(localStorage.getItem(BK_KEY)||'0')>0)
    toast('Backup automtico salvo ✓','success');
}
function updBackupStatus(){
  const el=document.getElementById('backupStatusTxt');
  const dot=document.getElementById('backupDot');
  const last=parseInt(localStorage.getItem(BK_KEY)||'0');
  if(!el)return;
  if(!last){el.textContent='Nunca feito';if(dot)dot.className='backup-dot warn';return;}
  const days=Math.floor((Date.now()-last)/(1000*60*60*24));
  el.textContent=days===0?'Hoje':days===1?'Ontem':`H ${days} dias`;
  if(dot)dot.className='backup-dot'+(days>=BK_INTERVAL_DAYS?' warn':'');
}

// ════════════════════════════════════════════════════════════
// COMPARATIVO MENSAL
// ════════════════════════════════════════════════════════════
function renderMonthCompare(){
  const el=document.getElementById('monthCompare');
  if(!el)return;
  const cur=curDt;
  const prev=new Date(cur.getFullYear(),cur.getMonth()-1,1);
  const txCur=getMonthTx(cur).filter(t=>!isFut(t.date)&&!t.paid);
  const txPrev=getMonthTx(prev).filter(t=>!isFut(t.date)&&!t.paid);
  const sum=(txs,type)=>txs.filter(t=>t.type===type).reduce((s,t)=>s+t.amount,0);
  const cInc=sum(txCur,'income'),cExp=sum(txCur,'expense');
  const pInc=sum(txPrev,'income'),pExp=sum(txPrev,'expense');
  const cSav=cInc-cExp, pSav=pInc-pExp;
  const delta=(cur,prev)=>{
    if(!prev)return{pct:null,cls:'neu'};
    const pct=Math.round(((cur-prev)/prev)*100);
    return{pct,cls:pct>0?'up':'dn',str:(pct>0?'▲':'▼')+Math.abs(pct)+'%'};
  };
  const dExp=delta(cExp,pExp);
  const dInc=delta(cInc,pInc);
  const dSav=delta(cSav,pSav);
  el.innerHTML=`
    <div class="month-compare">
      <div class="mc-card">
        <div class="mc-label">Receitas</div>
        <div class="mc-val" style="color:var(--ac)">${fmt(cInc)}</div>
        <div class="mc-delta ${dInc.cls}">${dInc.pct!==null?dInc.str+' vs mês ant.':'primeiro mês'}</div>
      </div>
      <div class="mc-card">
        <div class="mc-label">Despesas</div>
        <div class="mc-val" style="color:var(--dan)">${fmt(cExp)}</div>
        <div class="mc-delta ${dExp.cls==='up'?'up':'dn'}">${dExp.pct!==null?dExp.str+' vs mês ant.':'primeiro mês'}</div>
      </div>
    </div>
    <div class="month-compare" style="margin-bottom:0">
      <div class="mc-card">
        <div class="mc-label">Economizado</div>
        <div class="mc-val" style="color:${cSav>=0?'var(--ac2)':'var(--dan)'}">${fmt(cSav)}</div>
        <div class="mc-delta ${dSav.cls}">${dSav.pct!==null?dSav.str+' vs mês ant.':''}</div>
      </div>
      <div class="mc-card">
        <div class="mc-label">Ms anterior</div>
        <div class="mc-val" style="color:var(--mt);font-size:14px">${fmt(pExp)}</div>
        <div class="mc-delta neu">total gasto</div>
      </div>
    </div>`;
}

// ════════════════════════════════════════════════════════════
// PROJEO DE SALDO
// ════════════════════════════════════════════════════════════
function renderProjection(){
  const el=document.getElementById('projCard');
  if(!el)return;
  // Média dos últimos 3 meses
  let totalInc=0,totalExp=0,cnt=0;
  for(let i=1;i<=3;i++){
    const d=new Date(curDt.getFullYear(),curDt.getMonth()-i,1);
    const txs=getMonthTx(d).filter(t=>!isFut(t.date)&&!t.paid);
    const inc=txs.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
    const exp=txs.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
    if(inc>0||exp>0){totalInc+=inc;totalExp+=exp;cnt++;}
  }
  if(!cnt){el.innerHTML='';return;}
  const avgInc=totalInc/cnt,avgExp=totalExp/cnt,avgSav=avgInc-avgExp;
  const curBal=S.accounts.reduce((s,a)=>s+getAccBal(a.id),0);
  const proj3=curBal+(avgSav*3);
  const proj6=curBal+(avgSav*6);
  const trend=avgSav>=0?'positiva':'negativa';
  const trendColor=avgSav>=0?'var(--ac2)':'var(--dan)';
  el.innerHTML=`
    <div class="proj-card">
      <div class="proj-icon">🔭</div>
      <div class="proj-info">
        <div class="proj-title">Projeção de saldo</div>
        <div class="proj-detail">
          Tendência <strong style="color:${trendColor}">${trend}</strong>
          média mensal de <strong style="color:${avgSav>=0?'var(--ac)':'var(--dan)'}">${fmt(Math.abs(avgSav))}</strong>
          ${avgSav>=0?'economizados':'de dficit'}<br>
          Em 3 meses: <strong>${fmt(proj3)}</strong> &nbsp;&nbsp; Em 6 meses: <strong>${fmt(proj6)}</strong>
        </div>
      </div>
    </div>`;
}

// ════════════════════════════════════════════════════════════
// ALERTAS DE ORAMENTO
// ════════════════════════════════════════════════════════════
function renderBudAlerts(){
  const el=document.getElementById('budAlerts');
  if(!el||!S.budgets.length){if(el)el.innerHTML='';return;}
  const now=new Date();
  const alerts=[];
  S.budgets.forEach(b=>{
    const spent=S.transactions.filter(t=>{
      if(t.type!=='expense'||isFut(t.date)||t.paid)return false;
      const d=new Date(t.date+'T12:00:00');
      return d.getMonth()===now.getMonth()&&d.getFullYear()===now.getFullYear()&&t.category===b.category;
    }).reduce((s,t)=>s+t.amount,0);
    const pct=(spent/b.limit)*100;
    if(pct>=80)alerts.push({cat:b.category,spent,limit:b.limit,pct,over:pct>=100});
  });
  if(!alerts.length){el.innerHTML='';return;}
  el.innerHTML=alerts.map(a=>{
    const cat=getCat(a.cat);
    const cls=a.over?'bud-alert bud-alert-danger':'bud-alert';
    const msg=a.over
      ?`${cat.ico} <strong>${a.cat}</strong> excedeu o limite  gastou <strong>${fmt(a.spent)}</strong> de ${fmt(a.limit)}`
      :`${cat.ico} <strong>${a.cat}</strong> em <strong>${Math.round(a.pct)}%</strong> do orçamento (${fmt(a.spent)} de ${fmt(a.limit)})`;
    return`<div class="${cls}">⚠️ <span>${msg}</span></div>`;
  }).join('');
}

// ════════════════════════════════════════════════════════════
// EDIO INLINE
// ════════════════════════════════════════════════════════════
let _editingInline=null;
function openInlineEdit(id){
  // Fecha se j aberto
  if(_editingInline===id){closeInlineEdit();return;}
  closeInlineEdit();
  _editingInline=id;
  const tx=S.transactions.find(t=>t.id===id);
  if(!tx)return;
  const wrap=document.getElementById('ie-'+id);
  if(!wrap)return;
  wrap.classList.add('open');
  wrap.innerHTML=`
    <div class="ti-edit-row">
      <input class="ti-edit-inp desc" id="ie-desc-${id}" value="${tx.desc.replace(/"/g,'&quot;')}" placeholder="Descrição">
      <input class="ti-edit-inp amount" id="ie-amt-${id}" type="number" value="${tx.amount}" step="0.01" min="0.01">
      <input class="ti-edit-inp" id="ie-dt-${id}" type="date" value="${tx.date}" style="width:130px">
      <button class="btn btn-p btn-sm" onclick="saveInlineEdit('${id}')">✓</button>
      <button class="btn btn-g btn-sm" onclick="closeInlineEdit()">✕</button>
    </div>`;
  setTimeout(()=>document.getElementById('ie-amt-'+id)?.focus(),50);
}
function closeInlineEdit(){
  if(!_editingInline)return;
  const wrap=document.getElementById('ie-'+_editingInline);
  if(wrap){wrap.classList.remove('open');wrap.innerHTML='';}
  _editingInline=null;
}
async function saveInlineEdit(id){
  const tx=S.transactions.find(t=>t.id===id);
  if(!tx)return;
  const desc=document.getElementById('ie-desc-'+id)?.value.trim()||tx.desc;
  const amount=parseFloat(document.getElementById('ie-amt-'+id)?.value)||tx.amount;
  const date=document.getElementById('ie-dt-'+id)?.value||tx.date;
  if(amount<=0){toast('Valor invlido','error');return;}
  tx.desc=desc;tx.amount=amount;tx.date=date;
  if(cfg.mode==='api'){
    try{await api('PUT',`/api/transactions/${id}`,{type:tx.type,description:desc,amount,category:tx.category,date,note:tx.note||'',account_id:tx.accountId,paid:tx.paid,pending:tx.pending});}
    catch(e){toast('Erro: '+e.message,'error');return;}
  } else saveLocal();
  closeInlineEdit();
  refreshAll();
  toast('Atualizado ✓','success');
}


// ════════════════════════════════════════════════════════════
// NOTIFICAES LOCAIS (Capacitor)
// ════════════════════════════════════════════════════════════
async function initNotifications() {
  if (!window.Capacitor) return;
  try {
    const { LocalNotifications } = Capacitor.Plugins;
    if (!LocalNotifications) return;

    const perm = await LocalNotifications.requestPermissions();
    if (perm.display !== 'granted') return;

    // Cancela notificaes antigas antes de reagendar
    await LocalNotifications.cancel({ notifications: [{ id: 1 }, { id: 2 }, { id: 3 }] }).catch(() => {});

    scheduleVencimentoNotifications();
  } catch (e) {
    console.warn('Notificações não disponíveis:', e.message);
  }
}

async function scheduleVencimentoNotifications() {
  if (!window.Capacitor?.Plugins?.LocalNotifications) return;
  const { LocalNotifications } = Capacitor.Plugins;

  const hoje = today();
  const em3d = offD(new Date(), 3);
  const em7d = offD(new Date(), 7);

  // Pega contas a vencer nos próximos 7 dias
  const proximas = S.transactions.filter(t =>
    t.type === 'expense' && !t.paid && t.date > hoje && t.date <= em7d
  ).sort((a, b) => a.date.localeCompare(b.date));

  if (!proximas.length) return;

  const notifs = [];
  const agora = new Date();

  // Notificação diária às 9h se tiver contas a vencer hoje ou amanhã
  const urgentes = proximas.filter(t => t.date <= offD(new Date(), 1));
  if (urgentes.length) {
    const total = urgentes.reduce((s, t) => s + t.amount, 0);
    const schedDate = new Date();
    schedDate.setHours(9, 0, 0, 0);
    if (schedDate <= agora) schedDate.setDate(schedDate.getDate() + 1);
    notifs.push({
      id: 1,
      title: '⚠️ Contas vencendo',
      body: `${urgentes.length} conta${urgentes.length > 1 ? 's' : ''} a vencer: ${fmt(total)}`,
      schedule: { at: schedDate, repeats: false },
      sound: 'default',
      smallIcon: 'ic_stat_icon_config_sample',
    });
  }

  // Resumo semanal: domingo s 10h
  const domingo = new Date();
  domingo.setDate(domingo.getDate() + (7 - domingo.getDay()) % 7 || 7);
  domingo.setHours(10, 0, 0, 0);
  const totalSem = proximas.reduce((s, t) => s + t.amount, 0);
  notifs.push({
    id: 2,
    title: '📅 Finanza  semana',
    body: `${proximas.length} conta${proximas.length > 1 ? 's' : ''} nos próximos 7 dias: ${fmt(totalSem)}`,
    schedule: { at: domingo, repeats: false },
    sound: 'default',
    smallIcon: 'ic_stat_icon_config_sample',
  });

  try {
    await LocalNotifications.schedule({ notifications: notifs });
    console.log('✅ Notificações agendadas:', notifs.length);
  } catch (e) {
    console.warn('Erro ao agendar notificaes:', e.message);
  }
}

// ════════════════════════════════════════════════════════════
// DEEP LINK  shortcut "quick-add" abre o FAB direto
// ════════════════════════════════════════════════════════════
function initDeepLink() {
  if (!window.Capacitor) return;
  try {
    const { App } = Capacitor.Plugins;
    if (!App) return;

    // Ao abrir o app via shortcut
    App.addListener('appUrlOpen', (data) => {
      if (data.url?.includes('quick-add')) {
        // Navega para transações e abre o FAB
        setTimeout(() => {
          showPage('transactions');
          setTimeout(() => toggleQA(), 300);
        }, 500);
      }
    });

    // Verifica se foi aberto via shortcut na inicializao
    App.getLaunchUrl().then((data) => {
      if (data?.url?.includes('quick-add')) {
        setTimeout(() => {
          showPage('transactions');
          setTimeout(() => toggleQA(), 600);
        }, 1000);
      }
    }).catch(() => {});
  } catch (e) {
    console.warn('Deep link não disponível:', e.message);
  }
}


// ════════════════════════════════════════════════════════════
// SISTEMA DE WIDGETS DO DASHBOARD
// ════════════════════════════════════════════════════════════
const WIDGETS_KEY = 'fz_widgets';

// Definição de todos os widgets disponíveis
const WIDGET_DEFS = [
  { id:'cards',     ico:'💳', name:'Cards de saldo',       desc:'Saldo, receitas, despesas, a pagar', default:true },
  { id:'charts',    ico:'📊', name:'Gráficos',              desc:'Fluxo de caixa e categorias',        default:true },
  { id:'compare',   ico:'📅', name:'Comparativo mensal',    desc:'Este mês vs mês anterior',         default:true },
  { id:'projection',ico:'🔭', name:'Projeção de saldo',     desc:'Tendência dos próximos meses',       default:true },
  { id:'weekly',    ico:'📆', name:'Resumo semanal',        desc:'Gastos e economia da semana',        default:true },
  { id:'anomaly',   ico:'🚨', name:'Anomalias',             desc:'Categorias acima da média',          default:true },
  { id:'budalerts', ico:'⚠️', name:'Alertas de orçamento',  desc:'Limites próximos do teto',          default:true },
  { id:'goals',     ico:'🏆', name:'Metas rápidas',         desc:'Progresso das suas metas',           default:true },
  { id:'budgets',   ico:'🎯', name:'Orçamentos rápidos',    desc:'Uso mensal por categoria',           default:false },
  { id:'recent',    ico:'💸', name:'Últimas transações',    desc:'Lançamentos recentes',              default:true },
  { id:'ministats', ico:'📈', name:'Mini estatísticas',     desc:'Média diária, maior gasto, dias',   default:false },
  { id:'accounts',  ico:'🏦', name:'Saldos das contas',     desc:'Saldo de cada conta bancária',      default:false },
  { id:'shopping',  ico:'🛒', name:'Lista de compras',      desc:'Itens pendentes da lista ativa',    default:true },
  { id:'barcats',   ico:'📉', name:'Ranking de gastos',     desc:'Top categorias em barras',          default:false },
  { id:'heatmap',   ico:'🗓️', name:'Calendário de gastos',  desc:'Calor de gastos por dia do mês',    default:false },
  { id:'saverate',  ico:'💹', name:'Taxa de economia',      desc:'Quanto sobra das receitas',         default:false },
];

let widgetPrefs = {};
let widgetOrder = [];

function loadWidgetPrefs() {
  try {
    const s = localStorage.getItem(WIDGETS_KEY);
    widgetPrefs = asObj(s ? JSON.parse(s) : {});
  } catch { widgetPrefs = {}; }
  // Aplica defaults para widgets sem preferncia salva
  WIDGET_DEFS.forEach(w => {
    if (widgetPrefs[w.id] === undefined) widgetPrefs[w.id] = w.default;
  });
  try{widgetOrder=asArr(JSON.parse(localStorage.getItem(WIDGET_ORDER_KEY)||'[]'));}catch{widgetOrder=[];}
  const ids=WIDGET_DEFS.map(w=>w.id);
  widgetOrder=[...widgetOrder.filter(id=>ids.includes(id)),...ids.filter(id=>!widgetOrder.includes(id))];
}

function saveWidgetPrefs() {
  localStorage.setItem(WIDGETS_KEY, JSON.stringify(widgetPrefs));
  if(cfg.mode==='api')saveRemoteState().catch(()=>{});
}
function saveWidgetOrder(){localStorage.setItem(WIDGET_ORDER_KEY,JSON.stringify(widgetOrder));if(cfg.mode==='api')saveRemoteState().catch(()=>{});}
function isWidgetOn(id) {
  return widgetPrefs[id] !== false;
}

function toggleWidget(id) {
  widgetPrefs[id] = !widgetPrefs[id];
  saveWidgetPrefs();
  renderWidgetToggles();
  renderDash();
}
function resetWidgetOrder(){widgetOrder=WIDGET_DEFS.map(w=>w.id);saveWidgetOrder();renderDash();toast('Ordem do dashboard restaurada','info');}

function renderWidgetToggles() {
  const el = document.getElementById('widgetToggles');
  if (!el) return;
  el.innerHTML = WIDGET_DEFS.map(w => `
    <div class="widget-chip ${widgetPrefs[w.id] ? 'on' : ''}" onclick="toggleWidget('${w.id}')">
      <span class="wc-dot"></span>
      <span class="wc-ico">${w.ico}</span>
      <div class="wc-info">
        <div class="wc-name">${w.name}</div>
        <div class="wc-desc">${w.desc}</div>
      </div>
    </div>
  `).join('');
}

// ─── RENDER WIDGETS INDIVIDUAIS ──────────────────────────────

function renderDash() {
  const isDark = document.documentElement.dataset.theme === 'dark';
  const ids=WIDGET_DEFS.map(w=>w.id);
  widgetPrefs=asObj(widgetPrefs);
  widgetOrder=asArr(widgetOrder);
  widgetOrder=[...widgetOrder.filter(id=>ids.includes(id)),...ids.filter(id=>!widgetOrder.includes(id))];

  const renderers={
    cards:widgetCards,ministats:widgetMiniStats,accounts:widgetAccounts,shopping:widgetShoppingDash,
    compare:()=>'<div class="dash-section" id="monthCompare"></div>',
    projection:()=>'<div class="dash-section" id="projCard"></div>',
    weekly:()=>'<div class="dash-section" id="wsum"></div>',
    anomaly:()=>'<div class="dash-section" id="anom"></div>',
    budalerts:()=>'<div class="dash-section" id="budAlerts"></div>',
    saverate:widgetSaveRate,goals:widgetGoals,budgets:widgetBudgets,barcats:widgetBarCats,heatmap:widgetHeatmap,charts:widgetCharts,recent:widgetRecent
  };
  const wrap=(id,html)=>html?`<div class="dash-section-wrap" draggable="true" data-widget-id="${id}">
    <div class="widget-tools"><button class="widget-move-btn" onclick="moveWidgetStep('${id}',-1)" title="Subir">↑</button><button class="widget-move-btn" onclick="moveWidgetStep('${id}',1)" title="Descer">↓</button><button class="widget-drag-btn" title="Arrastar">↕</button><button class="widget-remove-btn" onclick="toggleWidget('${id}')" title="Remover widget"></button></div>${html}</div>`:'';
  const sections=widgetOrder.filter(id=>isWidgetOn(id)&&renderers[id]).map(id=>wrap(id,renderers[id]())).filter(Boolean);

  const container = document.getElementById('dashWidgets');
  if (container) container.innerHTML = sections.join('');
  initWidgetDrag();

  // Render sub-widgets que precisam de DOM pronto
  if (isWidgetOn('compare'))    renderMonthCompare();
  if (isWidgetOn('projection')) renderProjection();
  if (isWidgetOn('weekly'))     renderWeekly();
  if (isWidgetOn('anomaly'))    renderWeekly(); // renderWeekly j inclui anomaly
  if (isWidgetOn('budalerts'))  renderBudAlerts();
  if (isWidgetOn('charts'))     renderCharts(isDark);
}

function getDashWidgetItems(container){
  return [...container.querySelectorAll('.dash-section-wrap')];
}
function captureDashWidgetRects(container){
  return new Map(getDashWidgetItems(container).map(el=>[el.dataset.widgetId,el.getBoundingClientRect()]));
}
function animateDashWidgetsFrom(container,first){
  getDashWidgetItems(container).forEach(el=>{
    const old=first.get(el.dataset.widgetId);
    if(!old)return;
    const now=el.getBoundingClientRect();
    const dx=old.left-now.left,dy=old.top-now.top;
    if(!dx&&!dy)return;
    el.animate([
      {transform:`translate(${dx}px, ${dy}px)`},
      {transform:'translate(0, 0)'}
    ],{duration:280,easing:'cubic-bezier(.22,1,.36,1)'});
  });
}
function persistWidgetOrderFromDom(container){
  const visible=getDashWidgetItems(container).map(el=>el.dataset.widgetId);
  const hidden=widgetOrder.filter(id=>!visible.includes(id));
  widgetOrder=[...visible,...hidden];
  saveWidgetOrder();
}
function settleWidget(item){
  item.classList.add('drop-settle');
  setTimeout(()=>item.classList.remove('drop-settle'),320);
}
function moveWidgetStep(id,dir){
  const container=document.getElementById('dashWidgets');
  const item=container?.querySelector(`.dash-section-wrap[data-widget-id="${id}"]`);
  if(!container||!item)return;
  const target=dir<0?item.previousElementSibling:item.nextElementSibling;
  if(!target||!target.classList.contains('dash-section-wrap'))return;
  const first=captureDashWidgetRects(container);
  if(dir<0)container.insertBefore(item,target);
  else container.insertBefore(target,item);
  animateDashWidgetsFrom(container,first);
  persistWidgetOrderFromDom(container);
  settleWidget(item);
}

function initWidgetDrag(){
  const container=document.getElementById('dashWidgets');if(!container)return;
  let dragged=null;
  const widgetItems=()=>[...container.querySelectorAll('.dash-section-wrap')];
  const captureRects=()=>new Map(widgetItems().map(el=>[el.dataset.widgetId,el.getBoundingClientRect()]));
  const animateFrom=(first)=>{
    widgetItems().forEach(el=>{
      const old=first.get(el.dataset.widgetId);
      if(!old)return;
      const now=el.getBoundingClientRect();
      const dx=old.left-now.left,dy=old.top-now.top;
      if(!dx&&!dy)return;
      el.animate([
        {transform:`translate(${dx}px, ${dy}px)`},
        {transform:'translate(0, 0)'}
      ],{duration:280,easing:'cubic-bezier(.22,1,.36,1)'});
    });
  };
  const moveDragged=(target,e)=>{
    if(!dragged||!target||dragged===target)return;
    const rect=target.getBoundingClientRect();
    const after=e.clientY>rect.top+rect.height/2;
    const next=after?target.nextSibling:target;
    if(next===dragged||target.nextSibling===dragged&&after)return;
    const first=captureRects();
    container.insertBefore(dragged,next);
    animateFrom(first);
  };
  const persistOrder=()=>{
    const visible=widgetItems().map(el=>el.dataset.widgetId);
    const hidden=widgetOrder.filter(id=>!visible.includes(id));
    widgetOrder=[...visible,...hidden];
    saveWidgetOrder();
  };
  container.querySelectorAll('.dash-section-wrap').forEach(item=>{
    item.addEventListener('dragstart',e=>{
      dragged=item;
      container.classList.add('is-reordering');
      item.classList.add('dragging');
      e.dataTransfer.effectAllowed='move';
      e.dataTransfer.setData('text/plain',item.dataset.widgetId);
    });
    item.addEventListener('dragend',()=>{
      if(dragged){
        const settled=dragged;
        settled.classList.remove('dragging');
        settleWidget(settled);
      }
      container.classList.remove('is-reordering');
      container.querySelectorAll('.drag-over').forEach(x=>x.classList.remove('drag-over'));
      persistOrder();
      dragged=null;
    });
    item.addEventListener('dragover',e=>{
      e.preventDefault();
      if(item!==dragged){
        container.querySelectorAll('.drag-over').forEach(x=>{if(x!==item)x.classList.remove('drag-over');});
        item.classList.add('drag-over');
        moveDragged(item,e);
      }
    });
    item.addEventListener('dragleave',()=>item.classList.remove('drag-over'));
    item.addEventListener('drop',e=>{
      e.preventDefault();item.classList.remove('drag-over');
      moveDragged(item,e);
      persistOrder();
    });
  });
}

const SL_KEY = 'fz_shopping';

// Estrutura: { lists: [{id, name, ico}], items: [{id, listId, name, qty, cat, bought, createdAt}] }
let sl = { lists: [], items: [] };
let slActiveList = null;
let slNewListIco = '\u{1F6D2}';

function loadSL() {
  if(cfg.mode==='api'&&sl?.lists?.length)return;
  try { sl = JSON.parse(localStorage.getItem(SL_KEY) || 'null') || { lists:[], items:[] }; }
  catch { sl = { lists:[], items:[] }; }
  if (!sl.lists) sl.lists = [];
  if (!sl.items) sl.items = [];
  // Cria lista padro se no existir
  if (!sl.lists.length) {
    sl.lists.push({ id: uid(), name: 'Mercado', ico: '\u{1F6D2}' });
    saveSL();
  }
  slActiveList = sl.lists[0]?.id || null;
}

function saveSL() {
  localStorage.setItem(SL_KEY, JSON.stringify(sl));
  if(cfg.mode==='api')saveRemoteState().catch(e=>toast('Erro ao salvar lista: '+e.message,'error'));
}

function renderShopping() {
  loadSL();
  renderSLStats();
  renderSLTabs();
  renderSLContent();
}
function renderSLStats(){
  const el=document.getElementById('slStats');if(!el)return;
  const total=sl.items.length;
  const bought=sl.items.filter(i=>i.bought).length;
  const pending=total-bought;
  const active=sl.items.filter(i=>i.listId===slActiveList);
  const cats=new Set(sl.items.map(i=>i.cat)).size;
  el.innerHTML=`
    <div class="insight-card"><div class="insight-k">Pendentes</div><div class="insight-v" style="color:var(--warn)">${pending}</div><div class="cc">${total} itens totais</div></div>
    <div class="insight-card"><div class="insight-k">Comprados</div><div class="insight-v" style="color:var(--ac)">${bought}</div><div class="cc">${total?Math.round(bought/total*100):0}% concluído</div></div>
    <div class="insight-card"><div class="insight-k">Lista atual</div><div class="insight-v">${active.length}</div><div class="cc">itens nesta lista</div></div>
    <div class="insight-card"><div class="insight-k">Categorias</div><div class="insight-v" style="color:var(--ac2)">${cats}</div></div>`;
}

function renderSLTabs() {
  const el = document.getElementById('slTabs');
  if (!el) return;
  el.innerHTML = sl.lists.map(l => {
    const items = sl.items.filter(i => i.listId === l.id);
    const pending = items.filter(i => !i.bought).length;
    const isActive = l.id === slActiveList;
    return `<div class="sl-tab ${isActive ? 'active' : ''}" onclick="switchSLList('${l.id}')">
      <span>${l.ico}</span>
      <span>${l.name}</span>
      ${pending > 0 ? `<span class="sl-count">${pending}</span>` : ''}
      ${sl.lists.length > 1 ? `<span class="sl-tab-del" onclick="event.stopPropagation();deleteList('${l.id}')">✕</span>` : ''}
    </div>`;
  }).join('') + `<button class="sl-tab" onclick="openNewListModal()" style="color:var(--mt)">+ Lista</button>`;
}

function renderSLContent() {
  const el = document.getElementById('slContent');
  if (!el) return;

  const items = sl.items.filter(i => i.listId === slActiveList);
  const total = items.length;
  const bought = items.filter(i => i.bought).length;

  // Progress
  const fill = document.getElementById('slProgFill');
  const txt = document.getElementById('slProgTxt');
  if (fill) fill.style.width = total ? (bought/total*100)+'%' : '0%';
  if (txt) txt.textContent = `${bought}/${total}`;

  if (!total) {
    el.innerHTML = `<div class="sl-empty"><span class="sei">🛒</span><p>Lista vazia  adicione itens acima.</p></div>`;
    return;
  }

  // Agrupar por categoria
  const cats = {};
  items.forEach(i => {
    if (!cats[i.cat]) cats[i.cat] = [];
    cats[i.cat].push(i);
  });

  // Pendentes primeiro, depois comprados
  const sortedCats = Object.entries(cats).sort(([,a],[,b]) => {
    const aPend = a.some(x=>!x.bought);
    const bPend = b.some(x=>!x.bought);
    if (aPend && !bPend) return -1;
    if (!aPend && bPend) return 1;
    return 0;
  });

  el.innerHTML = sortedCats.map(([cat, catItems]) => {
    const pending = catItems.filter(i=>!i.bought).length;
    return `<div class="sl-cat-section">
      <div class="sl-cat-label">
        <span>${cat}</span>
        ${pending > 0 ? `<span style="color:var(--ac);font-size:9px">${pending} pendente${pending>1?'s':''}</span>` : `<span style="color:var(--grn);font-size:9px">✓ todos</span>`}
      </div>
      <div class="sl-items">
        ${catItems.sort((a,b)=>a.bought-b.bought).map(i => `
          <div class="sl-item ${i.bought?'bought':''}" onclick="toggleSLItem('${i.id}')">
            <div class="sl-check"><span class="sl-check-ico">✓</span></div>
            <span class="sl-name">${i.name}</span>
            ${i.qty ? `<span class="sl-qty">${i.qty}</span>` : ''}
            <button class="sl-item-del" onclick="event.stopPropagation();deleteSLItem('${i.id}')">🗑️</button>
          </div>`).join('')}
      </div>
    </div>`;
  }).join('');
}

function switchSLList(id) {
  slActiveList = id;
  if(cfg.mode==='api')saveRemoteState().catch(()=>{});
  renderSLStats();
  renderSLTabs();
  renderSLContent();
}

function addShoppingItem() {
  const nameEl = document.getElementById('slItemName');
  const qtyEl  = document.getElementById('slItemQty');
  const catEl  = document.getElementById('slItemCat');
  const name = nameEl?.value.trim();
  if (!name) { nameEl?.focus(); return; }
  if (!slActiveList) return;

  sl.items.push({
    id: uid(),
    listId: slActiveList,
    name,
    qty: qtyEl?.value.trim() || '',
    cat: catEl?.value || '🛒 Geral',
    bought: false,
    createdAt: Date.now()
  });
  saveSL();

  if (nameEl) nameEl.value = '';
  if (qtyEl)  qtyEl.value  = '';
  nameEl?.focus();

  renderSLTabs();
  renderSLStats();
  renderSLContent();
}

function toggleSLItem(id) {
  const item = sl.items.find(i => i.id === id);
  if (!item) return;
  item.bought = !item.bought;
  saveSL();
  renderSLStats();
  renderSLTabs();
  renderSLContent();
}

function deleteSLItem(id) {
  sl.items = sl.items.filter(i => i.id !== id);
  saveSL();
  renderSLStats();
  renderSLTabs();
  renderSLContent();
}

function clearBought() {
  if (!confirm('Remover todos os itens j comprados?')) return;
  sl.items = sl.items.filter(i => i.listId !== slActiveList || !i.bought);
  saveSL();
  renderSLStats();
  renderSLContent();
  toast('Comprados removidos ✓', 'success');
}

function openNewListModal() {
  document.getElementById('nlName').value = '';
  slNewListIco = '🛒';
  document.querySelectorAll('.list-ico-btn').forEach(b => b.classList.toggle('sel', b.textContent === '🛒'));
  document.getElementById('newListModal').classList.add('open');
  setTimeout(() => document.getElementById('nlName').focus(), 100);
}

function selListIco(btn, ico) {
  slNewListIco = ico;
  document.querySelectorAll('.list-ico-btn').forEach(b => b.classList.remove('sel'));
  btn.classList.add('sel');
}

function createNewList() {
  const name = document.getElementById('nlName').value.trim();
  if (!name) { toast('Informe o nome', 'error'); return; }
  const id = uid();
  sl.lists.push({ id, name, ico: slNewListIco });
  slActiveList = id;
  saveSL();
  closeM('newListModal');
  renderSLStats();
  renderSLTabs();
  renderSLContent();
  toast(`Lista "${name}" criada! ✓`, 'success');
}

function deleteList(id) {
  if (!confirm('Remover esta lista e todos os itens?')) return;
  sl.lists = sl.lists.filter(l => l.id !== id);
  sl.items = sl.items.filter(i => i.listId !== id);
  slActiveList = sl.lists[0]?.id || null;
  saveSL();
  renderSLStats();
  renderSLTabs();
  renderSLContent();
}

// ════════════════════════════════════════════════════════════
// NOTIFICAO PERSISTENTE (Android Capacitor)
// ════════════════════════════════════════════════════════════
async function setupPersistentNotification() {
  if (!window.Capacitor) return;
  const { LocalNotifications } = Capacitor.Plugins || {};
  if (!LocalNotifications) return;

  try {
    const perm = await LocalNotifications.checkPermissions();
    if (perm.display !== 'granted') return;

    // Cancela notificao persistente anterior
    await LocalNotifications.cancel({ notifications: [{ id: 99 }] }).catch(() => {});

    const saldo = S.accounts.reduce((s,a) => s + getAccBal(a.id), 0);
    const pendentes = S.transactions.filter(t => isFut(t.date) && !t.paid && t.type === 'expense').length;
    const slPending = (() => {
      try {
        const data = JSON.parse(localStorage.getItem(SL_KEY) || '{}');
        return (data.items || []).filter(i => !i.bought).length;
      } catch { return 0; }
    })();

  // Notificação persistente, ongoing + sem autoCancel + priority max
    await LocalNotifications.schedule({
      notifications: [{
        id: 99,
        title: `💰 Finanza  ${fmt(saldo)}`,
        body: `${pendentes > 0 ? pendentes + ' conta(s) a pagar' : 'Sem contas pendentes'}${slPending > 0 ? '  🛒 ' + slPending + ' itens na lista' : ''}`,
        schedule: { at: new Date(Date.now() + 500), repeats: false },
        ongoing: true,
        sticky: true,
        autoCancel: false,
        silent: true,
        foreground: true,
        sound: null,
        channelId: 'finanza_persistent',
        smallIcon: 'ic_stat_icon_config_sample',
        actionTypeId: 'FINANZA_ACTIONS',
        extra: { action: 'open' }
      }]
    });
  } catch (e) {
    console.warn('Persistent notification:', e.message);
  }
}

// Registrar action types para a notificao persistente
async function registerNotifActions() {
  if (!window.Capacitor) return;
  const { LocalNotifications } = Capacitor.Plugins || {};
  if (!LocalNotifications) return;
  try {
    // Criar canal Android com importncia mnima (sem som, sem pop-up, no pode fechar)
    await LocalNotifications.createChannel({
      id: 'finanza_persistent',
      name: 'Finanza Status',
      description: 'Saldo e atalhos do Finanza',
      importance: 2,       // IMPORTANCE_LOW  aparece mas no interrompe
      visibility: 1,       // VISIBILITY_PUBLIC
      sound: null,
      vibration: false,
      lights: false,
    }).catch(() => {});    // ignora se j existe

    await LocalNotifications.registerActionTypes({
      types: [{
        id: 'FINANZA_ACTIONS',
        actions: [
          { id: 'add_expense', title: '+ Gasto', foreground: true },
          { id: 'shopping',    title: '🛒 Lista', foreground: true },
        ]
      }]
    });
    // Listener para aes da notificao
    LocalNotifications.addListener('localNotificationActionPerformed', (notif) => {
      if (notif.actionId === 'add_expense') {
        showPage('transactions');
        setTimeout(() => toggleQA(), 400);
      } else if (notif.actionId === 'shopping') {
        showPage('shopping');
      } else {
        // Toque simples na notificao
        showPage('dashboard');
      }
    });
  } catch (e) {
    console.warn('Notif actions:', e.message);
  }
}

