// Componentes e widgets renderizados
// Extraido de index.html para organizar o frontend sem mudar a arquitetura global.

function widgetCards() {
  const totBal = S.accounts.reduce((s,a) => s + getAccBal(a.id), 0);
  const yld = getYieldSummary();
  const txM = getMonthTx(curDt);
  const inc = txM.filter(t => t.type==='income'&&!isFut(t.date)&&!t.paid).reduce((s,t)=>s+t.amount,0);
  const exp = txM.filter(t => t.type==='expense'&&!isFut(t.date)&&!t.paid).reduce((s,t)=>s+t.amount,0);
  const fut = S.transactions.filter(t=>t.type==='expense'&&isFut(t.date)&&!t.paid).reduce((s,t)=>s+t.amount,0);
  // Store for other widgets
  window._dashInc = inc; window._dashExp = exp;
  return `<div class="g4 summary-grid dash-section">
    <div class="sc sc-glow-ac2"><span class="ci">💰</span><div class="cl">Saldo Total</div><div class="cv ${totBal>=0?'neu':'neg'}">${fmt(totBal)}</div><div class="cc">todas as contas</div></div>
    <div class="sc sc-glow-ac"><span class="ci">🌱</span><div class="cl">Rendendo</div><div class="cv pos">${fmt(yld.month)}</div><div class="cc up">estimado este mês</div><div class="yield-detail"><span>${fmt(yld.day)}/dia</span><span>•</span><span>${fmt(yld.year)}/ano</span></div></div>
    <div class="sc sc-glow-ac"><span class="ci">⬆️</span><div class="cl">Receitas</div><div class="cv pos">${fmt(inc)}</div><div class="cc up">este mês</div></div>
    <div class="sc sc-glow-dan"><span class="ci">⬇️</span><div class="cl">Despesas</div><div class="cv neg">${fmt(exp)}</div><div class="cc dn">este mês</div></div>
    <div class="sc sc-glow-fut" style="cursor:pointer" onclick="showPage('future')"><span class="ci">🔮</span><div class="cl">A Pagar</div><div class="cv fut">${fmt(fut)}</div><div class="cc" style="color:var(--fut)">ver →</div></div>
  </div>`;
}

function widgetMiniStats() {
  const txM = getMonthTx(curDt).filter(t=>!isFut(t.date)&&!t.paid&&t.type==='expense');
  if (!txM.length) return '';
  const total = txM.reduce((s,t)=>s+t.amount,0);
  const dias = new Set(txM.map(t=>t.date)).size;
  const media = total / Math.max(dias, 1);
  const maior = Math.max(...txM.map(t=>t.amount));
  const diasMes = new Date(curDt.getFullYear(), curDt.getMonth()+1, 0).getDate();
  const diasRest = diasMes - curDt.getDate();
  return `<div class="mini-stats dash-section">
    <div class="mini-stat"><div class="ms-ico">📊</div><div class="ms-val" style="color:var(--warn)">${fmt(media)}</div><div class="ms-lbl">Média/dia</div></div>
    <div class="mini-stat"><div class="ms-ico">🔺</div><div class="ms-val" style="color:var(--dan)">${fmt(maior)}</div><div class="ms-lbl">Maior gasto</div></div>
    <div class="mini-stat"><div class="ms-ico">📅</div><div class="ms-val" style="color:var(--ac2)">${diasRest}</div><div class="ms-lbl">Dias restantes</div></div>
  </div>`;
}

function widgetAccounts() {
  if (!S.accounts.length) return '';
  return `<div class="box dash-section">
    <div class="bh"><div class="ct">Contas</div><button class="btn btn-g btn-sm" onclick="showPage('accounts')">Ver →</button></div>
    <div style="display:flex;flex-direction:column;gap:8px;">
      ${S.accounts.map(a => {
        const bal = getAccBal(a.id);
        const y = getAccYield(a,1);
        return `<div style="display:flex;align-items:center;justify-content:space-between;padding:8px 4px;border-bottom:1px solid var(--bd2)">
          <div style="display:flex;align-items:center;gap:8px"><span style="font-size:18px">${a.icon}</span><span style="font-size:13px;font-weight:500">${a.name}</span>${y>0?`<span class="yield-pill">+${fmt(y)}</span>`:''}</div>
          <span style="font-family:var(--font-money);font-weight:700;font-size:14px;color:${bal>=0?'var(--ac)':'var(--dan)'}">${fmt(bal)}</span>
        </div>`;
      }).join('')}
    </div>
  </div>`;
}

function widgetGoals() {
  const goals = S.goals.filter(g => (g.current/g.target) < 1).slice(0, 4);
  if (!goals.length) return '';
  return `<div class="goals-widget dash-section">
    <div class="bh"><div class="ct">🏆 Metas</div><button class="btn btn-g btn-sm" onclick="showPage('goals')">Ver todas →</button></div>
    <div class="goals-widget-list">
      ${goals.map(g => {
        const pct = Math.min((g.current/g.target)*100, 100);
        const col = pct>=80?'var(--ac)':pct>=50?'var(--ac2)':'var(--warn)';
        return `<div class="gw-item">
          <span class="gw-icon">${g.icon}</span>
          <div class="gw-info">
            <div class="gw-name">${g.name}</div>
            <div class="gw-bar"><div class="gw-fill" style="width:${pct}%;background:${col}"></div></div>
          </div>
          <span class="gw-pct" style="color:${col}">${Math.round(pct)}%</span>
        </div>`;
      }).join('')}
    </div>
  </div>`;
}

function widgetBudgets() {
  const now = new Date();
  const buds = S.budgets.map(b => {
    const spent = S.transactions.filter(t => {
      if (t.type!=='expense'||isFut(t.date)||t.paid) return false;
      const d = new Date(t.date+'T12:00:00');
      return d.getMonth()===now.getMonth()&&d.getFullYear()===now.getFullYear()&&t.category===b.category;
    }).reduce((s,t)=>s+t.amount,0);
    return {...b, spent, pct: Math.min((spent/b.limit)*100, 100)};
  }).sort((a,b)=>b.pct-a.pct).slice(0,6);
  if (!buds.length) return '';
  return `<div class="budget-widget dash-section">
    <div class="bh"><div class="ct">🎯 Orçamentos</div><button class="btn btn-g btn-sm" onclick="showPage('budget')">Ver todos →</button></div>
    <div class="bw-list">
      ${buds.map(b => {
        const cat = getCat(b.category);
        const col = b.pct>=100?'var(--dan)':b.pct>=80?'var(--warn)':'var(--ac)';
        return `<div class="bw-item">
          <span class="bw-cat">${cat.ico}</span>
          <div class="bw-info">
            <div class="bw-top">
              <span class="bw-name">${b.category}</span>
              <span class="bw-vals">${fmt(b.spent)} / ${fmt(b.limit)}</span>
            </div>
            <div class="bw-bar"><div class="bw-fill" style="width:${b.pct}%;background:${col}"></div></div>
          </div>
        </div>`;
      }).join('')}
    </div>
  </div>`;
}

function widgetCharts() {
  return `<div class="cr dash-section">
    <div class="cc-box">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:2px;">
        <div class="ct">Fluxo de Caixa</div>
        <div style="display:flex;gap:3px;">
          <button class="vbtn ${chartMode==='bars'?'active':''}" id="cBars" onclick="setChartMode('bars')" title="Barras">▦</button>
          <button class="vbtn ${chartMode==='line'?'active':''}" id="cLine" onclick="setChartMode('line')" title="Acumulado">📈</button>
        </div>
      </div>
      <div class="cs" id="chartSub">${chartMode==='bars'?'Receitas vs Despesas — últimos 6 meses':'Saldo acumulado — últimos 6 meses'}</div>
      <canvas id="flowChart"></canvas>
    </div>
    <div class="cc-box"><div class="ct">Categorias</div><div class="cs">Clique para filtrar</div><canvas id="catChart" style="cursor:pointer"></canvas></div>
  </div>`;
}

function widgetRecent() {
  const sorted = [...S.transactions]
    .filter(t=>!isFut(t.date)&&!t.paid)
    .sort((a,b)=>b.date.localeCompare(a.date))
    .slice(0,6);
  return `<div class="box dash-section">
    <div class="bh"><div class="ct">Últimas Transações</div><button class="btn btn-g btn-sm" onclick="showPage('transactions')">Ver todas →</button></div>
    <div class="recent-list" id="recList">${sorted.length ? sorted.map(recentTxHTML).join('') : '<div class="empty"><span class="ei">💸</span><p>Nenhuma transação ainda.</p></div>'}</div>
  </div>`;
}

function recentTxHTML(tx) {
  const cat = getCat(tx.category);
  const acc = S.accounts.find(a => a.id === tx.accountId);
  const sign = tx.type === 'income' ? '+' : '-';
  const tone = tx.type === 'income' ? 'income' : 'expense';
  return `<div class="recent-tx" onclick="showPage('transactions');setTimeout(()=>hlTx('${tx.id}'),250)">
    <div class="recent-ico" style="background:${cat.col}20;color:${cat.col}">${cat.ico}</div>
    <div class="recent-main">
      <div class="recent-name">${tx.desc}</div>
      <div class="recent-meta"><span>${tx.category}</span><span>${fmtD(tx.date)}</span>${acc?`<span>${acc.icon}</span>`:''}</div>
    </div>
    <div class="recent-amt ${tone}">${sign}${fmt(tx.amount)}</div>
  </div>`;
}

function widgetShoppingDash() {
  let data={lists:[],items:[]};
  try{data=JSON.parse(localStorage.getItem('fz_shopping')||'{}');}catch{}
  const lists=data.lists||[], items=data.items||[];
  const pending=items.filter(i=>!i.bought);
  const list=lists[0];
  const listItems=items.filter(i=>i.listId===list?.id);
  const bought=listItems.filter(i=>i.bought).length;
  const pct=listItems.length?Math.round(bought/listItems.length*100):0;
  return `<div class="box dash-section">
    <div class="bh"><div><div class="ct">🛒 ${list?.ico||'🛒'} ${list?.name||'Lista de Compras'}</div><div class="cs">${pending.length} pendente${pending.length!==1?'s':''}</div></div><button class="btn btn-g btn-sm" onclick="showPage('shopping')">Abrir →</button></div>
    <div class="sl-prog-bar" style="margin-bottom:12px"><div class="sl-prog-fill" style="width:${pct}%"></div></div>
    ${pending.slice(0,5).map(i=>`<div style="display:flex;align-items:center;gap:8px;padding:7px 2px;border-bottom:1px solid var(--bd2)"><div style="width:16px;height:16px;border-radius:5px;border:2px solid var(--bd);flex-shrink:0"></div><span style="font-size:13px;flex:1;min-width:0">${i.name}</span>${i.qty?`<span style="font-size:11px;color:var(--mt)">${i.qty}</span>`:''}</div>`).join('')||'<div class="empty" style="padding:18px"><p>Lista sem pendências.</p></div>'}
  </div>`;
}

function widgetSaveRate() {
  const months=[];
  for(let i=2;i>=0;i--){
    const d=new Date(curDt.getFullYear(),curDt.getMonth()-i,1);
    const txs=getMonthTx(d).filter(t=>!isFut(t.date)&&!t.paid);
    const inc=txs.filter(t=>t.type==='income').reduce((s,t)=>s+t.amount,0);
    const exp=txs.filter(t=>t.type==='expense').reduce((s,t)=>s+t.amount,0);
    if(inc>0)months.push({label:['Jan','Fev','Mar','Abr','Mai','Jun','Jul','Ago','Set','Out','Nov','Dez'][d.getMonth()],rate:Math.round((1-exp/inc)*100),inc,exp});
  }
  if(!months.length)return '';
  const cur=months[months.length-1], col=cur.rate>=20?'var(--grn)':cur.rate>=0?'var(--warn)':'var(--dan)';
  return `<div class="box dash-section"><div class="bh"><div class="ct">💹 Taxa de Economia</div></div><div style="display:flex;align-items:center;gap:16px;flex-wrap:wrap"><div style="text-align:center"><div style="font-family:var(--font-money);font-size:42px;font-weight:800;color:${col};letter-spacing:0">${cur.rate}%</div><div style="font-size:11px;color:var(--mt)">este mês</div></div><div style="flex:1;min-width:120px">${months.map(m=>{const c=m.rate>=20?'var(--grn)':m.rate>=0?'var(--warn)':'var(--dan)';return `<div style="display:flex;align-items:center;gap:8px;margin-bottom:6px"><span style="font-size:11px;color:var(--mt);width:28px">${m.label}</span><div style="flex:1;height:8px;background:var(--sf2);border-radius:99px;overflow:hidden"><div style="width:${Math.min(Math.abs(m.rate),100)}%;height:100%;background:${c};border-radius:99px"></div></div><span style="font-size:11px;font-weight:700;color:${c};width:34px;text-align:right">${m.rate}%</span></div>`;}).join('')}</div></div></div>`;
}

function widgetBarCats() {
  const txM=getMonthTx(curDt).filter(t=>t.type==='expense'&&!isFut(t.date)&&!t.paid);
  const cats={};txM.forEach(t=>cats[t.category]=(cats[t.category]||0)+t.amount);
  const sorted=Object.entries(cats).sort((a,b)=>b[1]-a[1]).slice(0,6);
  if(!sorted.length)return '';
  const max=sorted[0][1];
  return `<div class="box dash-section"><div class="bh"><div><div class="ct">📉 Top Categorias</div><div class="cs">Maiores gastos do mês</div></div></div><div style="display:flex;flex-direction:column;gap:10px">${sorted.map(([cat,val],i)=>{const c=getCat(cat),pct=(val/max*100).toFixed(0),cols=['var(--dan)','var(--warn)','var(--ac)','var(--ac2)','var(--fut)','var(--grn)'],col=cols[i]||'var(--mt)';return `<div><div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px"><span style="font-size:13px">${c.ico} ${cat}</span><span style="font-family:var(--font-money);font-size:13px;font-weight:700;color:${col}">${fmt(val)}</span></div><div style="height:6px;background:var(--sf2);border-radius:99px;overflow:hidden"><div style="width:${pct}%;height:100%;background:${col};border-radius:99px"></div></div></div>`;}).join('')}</div></div>`;
}

function widgetHeatmap() {
  const d=new Date(curDt.getFullYear(),curDt.getMonth(),1);
  const dim=new Date(d.getFullYear(),d.getMonth()+1,0).getDate();
  const txM=getMonthTx(curDt).filter(t=>t.type==='expense'&&!isFut(t.date)&&!t.paid);
  const dayTotals={};txM.forEach(t=>dayTotals[t.date]=(dayTotals[t.date]||0)+t.amount);
  const vals=Object.values(dayTotals),max=vals.length?Math.max(...vals):1,first=d.getDay();
  return `<div class="box dash-section"><div class="bh"><div><div class="ct">🗓️ Gastos do mês</div><div class="cs">Intensidade por dia</div></div></div><div class="heat-grid">${Array(first).fill('<div></div>').join('')}${Array.from({length:dim},(_,i)=>{const day=i+1,ds=`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(day).padStart(2,'0')}`,val=dayTotals[ds]||0,alpha=val?Math.max(.12,val/max):0;return `<div class="heat-cell" style="background:${val?`rgba(245,112,90,${alpha})`:'var(--sf2)'};color:${val?'#fff':'var(--mt)'}" title="${val?fmt(val):''}">${day}</div>`;}).join('')}</div></div>`;
}

// ─── RENDER DASH PRINCIPAL ───────────────────────────────────
