import {api,query} from './api.js';
import {esc,heading,options,genderNames,statusNames,orderBadge,money,date,swatch,pagination,empty,formModal,detailModal,closeModal,field,toast,errorText} from './ui.js';
const syncTime=()=>new Intl.DateTimeFormat('zh-CN',{timeZone:'Asia/Shanghai',hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false}).format(new Date());
async function pagedData(path,state,isCurrent){
 while(isCurrent()){
  const data=await api(path+'?'+query(state));
  if(!isCurrent())return null;
  const lastPage=Math.max(1,Math.ceil(data.total/state.pageSize));
  if(state.page>lastPage){state.page=lastPage;continue;}
  return data;
 }
 return null;
}
function userForm(user,onSave){
 formModal('编辑用户信息',
 field('手机号','<input name="phone" required inputmode="numeric" pattern="[0-9]{11}" maxlength="11" value="'+esc(user.phone)+'">')+
 field('昵称','<input name="nickname" required maxlength="40" value="'+esc(user.nickname)+'">')+
 field('性别','<select name="gender">'+options(Object.entries(genderNames).map(([id,name])=>({id,name})),user.gender)+'</select>')+
 field('生日','<input name="birthday" type="date" max="'+new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai'}).format(new Date())+'" value="'+esc(user.birthday)+'">'),
 async data=>onSave({phone:data.get('phone'),nickname:data.get('nickname').trim(),gender:data.get('gender'),birthday:data.get('birthday')||null}));
}
export async function userList(root){
 const state={page:1,pageSize:20,phone:''};
 let requestVersion=0,refreshing=false;
 root.innerHTML=heading('用户管理','通过手机号查看用户信息、已开通渠道及模拟订单。')+'<section class="panel"><form class="filters"><label>手机号<input name="phone" inputmode="numeric" maxlength="11" placeholder="输入全部或部分手机号"></label><button type="submit" class="primary">查询</button><button type="button" data-reset>重置</button><span class="filter-spacer"></span><span class="help" id="user-sync">自动同步中</span></form><div id="user-results"><div class="loading">正在读取用户…</div></div></section>';
 async function loadResults(){
  const version=++requestVersion;
  const data=await pagedData('/users',state,()=>version===requestVersion&&root.isConnected);
  if(!data)return;
  const area=root.querySelector('#user-results');if(!area)return;
  area.innerHTML=(data.items.length?'<div class="table-wrap"><table><thead><tr><th>用户</th><th>手机号</th><th>性别</th><th>生日</th><th>已开通渠道</th><th>操作</th></tr></thead><tbody>'+data.items.map(u=>'<tr><td><div class="user-cell"><span class="user-avatar">'+esc(u.nickname.charAt(0))+'</span><strong>'+esc(u.nickname)+'</strong></div></td><td class="mono">'+esc(u.phone)+'</td><td>'+genderNames[u.gender]+'</td><td class="muted">'+esc(u.birthday||'—')+'</td><td>'+u.channelCount+' 个渠道</td><td><a class="text-button" href="#/users/'+u.id+'">查看详情 →</a></td></tr>').join('')+'</tbody></table></div>':empty('没有找到用户','请检查手机号，或输入部分数字查询。'))+pagination(data);
  root.querySelector('#user-sync').textContent='已更新 · '+syncTime();
 }
 root.querySelector('form').onsubmit=async e=>{e.preventDefault();state.phone=new FormData(e.target).get('phone');state.page=1;try{await loadResults();}catch(err){toast(errorText(err),true);}};
 root.addEventListener('click',async e=>{
  const b=e.target.closest('button');if(!b)return;
  try{
   if(b.dataset.page){state.page=Number(b.dataset.page);await loadResults();}
   if(b.hasAttribute('data-reset')){state.phone='';state.page=1;root.querySelector('[name="phone"]').value='';await loadResults();}
  }catch(err){toast(errorText(err),true);}
 });
 await loadResults();
 return async()=>{if(refreshing||!root.isConnected)return;refreshing=true;try{await loadResults();}finally{refreshing=false;}};
}
function orderDetailBody(o){
 return '<dl class="order-detail">'+[['订单号',o.orderNo,true],['商品',o.productName,true],['款号',o.productCode],['渠道',o.channelName],['配色 / 尺码',o.colorName+' / '+o.size],['数量',o.quantity+' 件'],['成交单价',money(o.unitPrice)],['订单金额',money(o.totalPrice)],['订单状态',statusNames[o.status]],['下单用户',o.nickname+' · '+o.phone],['创建时间',date(o.createdAt)],['更新时间',date(o.updatedAt)]].map(([k,v,full])=>'<div class="'+(full?'full':'')+'"><dt>'+esc(k)+'</dt><dd>'+esc(v)+'</dd></div>').join('')+'</dl>';
}
export async function userDetail(root,id){
 let user,channels,requestVersion=0,detailVersion=0,refreshing=false,missing=false;
 const modal=document.querySelector('#modal'),state={page:1,pageSize:20,channelId:'',status:''};
 function handleMissingUser(err){
  if(err.status!==404)return false;
  missing=true;++requestVersion;++detailVersion;
  if(root.isConnected){closeModal();toast('该模拟用户已被清理，已返回用户列表。');location.hash='#/users';}
  return true;
 }
 async function loadOrders(){
  const version=++requestVersion;
  let data;
  try{data=await pagedData('/users/'+id+'/orders',state,()=>version===requestVersion&&root.isConnected&&!missing);}
  catch(err){if(handleMissingUser(err))return;throw err;}
  if(!data)return;
  const area=root.querySelector('#order-results');if(!area)return;
  area.innerHTML=(data.items.length?'<div class="table-wrap"><table><thead><tr><th>订单 / 创建时间</th><th>商品</th><th>规格</th><th>金额</th><th>状态</th><th>操作</th></tr></thead><tbody>'+data.items.map(o=>'<tr><td><span class="mono" title="'+esc(o.orderNo)+'">'+esc(o.orderNo.slice(0,14))+'…</span><div class="help">'+date(o.createdAt)+'</div></td><td><strong>'+esc(o.productName)+'</strong><div class="help">'+esc(o.channelName)+' · '+esc(o.productCode)+'</div></td><td><span class="swatch-label">'+swatch(o)+esc(o.colorName)+'</span><div class="help">'+o.size+' / '+o.quantity+' 件</div></td><td class="price nowrap">'+money(o.totalPrice)+'</td><td>'+orderBadge(o.status)+'</td><td><button class="text-button" data-order="'+o.id+'">详情</button></td></tr>').join('')+'</tbody></table></div>':empty('暂无订单','打开右上角任一模拟生成开关，订单会自动显示在这里。'))+pagination(data);
  root.querySelector('#order-sync').textContent='已更新 · '+syncTime();
 }
 async function syncOrderModal(){
  const orderId=modal.dataset.orderId,body=modal.querySelector('.modal-body');
  if(!modal.open||!orderId||missing)return;
  const isCurrent=()=>root.isConnected&&modal.open&&modal.dataset.orderId===orderId&&modal.querySelector('.modal-body')===body;
  try{const order=await api('/orders/'+orderId);if(isCurrent())body.innerHTML=orderDetailBody(order);}
  catch(err){if(!isCurrent())return;if(err.status===404){closeModal();toast('该订单已被模拟器清理。');}else throw err;}
 }
 async function render(){
  try{[user,channels]=await Promise.all([api('/users/'+id),api('/users/'+id+'/channels')]);}
  catch(err){if(handleMissingUser(err))return false;throw err;}
  if(!root.isConnected||missing)return false;
  root.innerHTML='<a class="back-link" href="#/users">← 返回用户列表</a><section class="panel product-summary user-summary"><span class="user-avatar">'+esc(user.nickname.charAt(0))+'</span><div class="summary-body"><h1>'+esc(user.nickname)+'</h1><div class="summary-meta"><span class="mono">'+esc(user.phone)+'</span><span class="muted">'+genderNames[user.gender]+'</span><span class="muted">生日 '+esc(user.birthday||'未设置')+'</span></div></div><button data-edit>编辑信息</button></section><section class="panel"><div class="panel-title"><h2>渠道订单 <small>'+channels.length+' 个已开通渠道</small></h2><span class="help" id="order-sync">自动同步中</span></div><div class="tabs"><button data-channel="" class="'+(state.channelId===''?'active':'')+'">全部渠道</button>'+channels.map(c=>'<button data-channel="'+c.id+'" class="'+(String(c.id)===String(state.channelId)?'active':'')+'">'+esc(c.name)+'</button>').join('')+'</div><form class="filters"><label>订单状态<select name="status">'+options(Object.entries(statusNames).map(([id,name])=>({id,name})),state.status,'全部状态')+'</select></label><button type="submit">筛选</button><span class="filter-spacer"></span><span class="help">状态由模拟器自动更新</span></form><div id="order-results"><div class="loading">正在读取订单…</div></div></section>';
  root.querySelector('form').onsubmit=async e=>{e.preventDefault();state.status=new FormData(e.target).get('status');state.page=1;try{await loadOrders();}catch(err){toast(errorText(err),true);}};
  await loadOrders();return !missing;
 }
 root.addEventListener('click',async e=>{
  const b=e.target.closest('button');if(!b||missing)return;
  const version=++detailVersion;
  try{
   if(b.hasAttribute('data-edit'))userForm(user,async body=>{try{await api('/users/'+id,{method:'PATCH',body});return await render();}catch(err){if(handleMissingUser(err))return false;throw err;}});
   if(b.hasAttribute('data-channel')){state.channelId=b.dataset.channel;state.page=1;root.querySelectorAll('[data-channel]').forEach(t=>t.classList.toggle('active',t===b));await loadOrders();}
   if(b.dataset.page){state.page=Number(b.dataset.page);await loadOrders();}
   if(b.dataset.order){
    const order=await api('/orders/'+b.dataset.order);
    if(!root.isConnected||missing||version!==detailVersion||modal.open)return;
    detailModal('订单详情',orderDetailBody(order)).dataset.orderId=String(order.id);
   }
  }catch(err){if(b.dataset.order&&err.status===404){toast('该订单已被模拟器清理。');try{await loadOrders();}catch(refreshError){toast(errorText(refreshError),true);}}else toast(errorText(err),true);}
 });
 await render();
 return async()=>{if(refreshing||!root.isConnected||missing)return;refreshing=true;try{await loadOrders();await syncOrderModal();}finally{refreshing=false;}};
}
