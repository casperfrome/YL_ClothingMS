import {api} from './api.js';
import {showError,toast,errorText,confirmAction,closeModal} from './ui.js';
import {productList,productDetail} from './products.js';
import {userList,userDetail} from './users.js';
import {categoriesPage,channelsPage} from './catalogs.js';
const app=document.querySelector('#app'),banner=document.querySelector('#connection-banner');
const simulators=[
 {kind:'data',label:'模拟数据生成',path:'/data-simulator',toggle:document.querySelector('#data-sim-toggle'),description:document.querySelector('#data-sim-description'),retainToggle:document.querySelector('#data-sim-retain-toggle'),retainDescription:document.querySelector('#data-sim-retain-description'),state:null,connectionError:''},
 {kind:'order',label:'模拟订单生成',path:'/order-simulator',toggle:document.querySelector('#sim-toggle'),description:document.querySelector('#sim-description'),retainToggle:document.querySelector('#sim-retain-toggle'),retainDescription:document.querySelector('#sim-retain-description'),state:null,connectionError:''}
];
let refresh=null,routeVersion=0,statusVersion=0,polling=false,changing=false,savingRetention=null,pageError='';
const titles={products:'商品管理',categories:'品类管理',users:'用户管理',channels:'渠道管理'};
async function route(){
 const version=++routeVersion;refresh=null;pageError='';closeModal();renderSimulatorState();
 const [name='products',id]=location.hash.replace(/^#\/?/,'').split('/');
 const section=titles[name]?name:'products';
 document.querySelectorAll('[data-nav]').forEach(a=>{a.classList.toggle('active',a.dataset.nav===section);a.setAttribute('aria-current',a.dataset.nav===section?'page':'false');});
 document.querySelector('#breadcrumb').textContent='工作空间 / '+titles[section]+(id?' / 详情':'');
 document.title=titles[section]+' · YL商品管理后台';
 app.innerHTML='<div class="screen"><div class="loading">正在加载…</div></div>';const root=app.firstElementChild;
 try{
  let callback=null;
  if(section==='products'){const [categories,channels]=await Promise.all([api('/categories'),api('/channels')]);if(id)await productDetail(root,id,{categories,channels});else await productList(root,{categories,channels});}
  else if(section==='categories')await categoriesPage(root);
  else if(section==='users')callback=id?await userDetail(root,id):await userList(root);
  else {await channelsPage(root);callback=()=>channelsPage(root);}
  if(version===routeVersion)refresh=callback;
 }catch(e){if(version===routeVersion)showError(root,e);}
}
function renderSimulatorState(){
 const messages=[];
 for(const simulator of simulators){
  const {state,toggle,description,retainToggle,retainDescription,connectionError,kind,label}=simulator;
  toggle.setAttribute('aria-checked',String(!!state?.enabled));
  toggle.disabled=changing||!state||!!connectionError;
  retainToggle.setAttribute('aria-checked',String(!!state?.retainData));
  retainToggle.disabled=changing||!state||!!connectionError;
  retainToggle.setAttribute('aria-busy',String(savingRetention===simulator));
  retainDescription.textContent=savingRetention===simulator?'正在保存…':connectionError?'连接中断 · 自动重试':!state?'连接中…':state.retainData?'停止后保留':'停止后清理';
  if(connectionError){description.textContent='连接中断 · 自动重试';messages.push(label+'：'+connectionError);continue;}
  if(!state){description.textContent='连接中…';continue;}
  const count=kind==='data'?'本轮 '+(state.generatedUserCount||0)+' 人 / '+state.generatedCount+' 单':'本轮生成 '+state.generatedCount+' 单';
  const labels={STOPPED:'已关闭',RUNNING:'运行中 · '+count,WAITING:kind==='data'?'等待可售商品 · '+count:'等待可售商品与已开通用户',ERROR:'生成失败 · 正在重试'};
  description.textContent=labels[state.state]||'状态未知';
  if(state.blockedReason||state.error)messages.push(label+'：'+(state.blockedReason||state.error));
 }
 if(pageError)messages.push('当前页面同步失败：'+pageError);
 banner.hidden=messages.length===0;banner.textContent=messages.join('；');
}
async function syncSimulators(){
 const version=++statusVersion;
 const results=await Promise.allSettled(simulators.map(simulator=>api(simulator.path)));
 if(version!==statusVersion)return;
 results.forEach((result,index)=>{
  const simulator=simulators[index];
  if(result.status==='fulfilled'){simulator.state=result.value;simulator.connectionError='';}
  else simulator.connectionError=errorText(result.reason);
 });
 renderSimulatorState();
}
async function refreshPage(){
 const version=routeVersion,callback=refresh;
 if(!callback)return;
 try{await callback();if(version===routeVersion)pageError='';}
 catch(e){if(version===routeVersion)pageError=errorText(e);}
 renderSimulatorState();
}
async function poll(){
 if(polling||changing)return;polling=true;
 try{await syncSimulators();if(!changing)await refreshPage();}
 finally{polling=false;}
}
function confirmationState(simulator,enabled){
 return JSON.stringify(simulators.filter(item=>item===simulator||enabled).map(item=>[item.kind,!!item.state?.enabled,!!item.state?.retainData]));
}
async function setSimulator(simulator,enabled,confirmedState){
 if(changing)return;
 changing=true;++statusVersion;renderSimulatorState();
 try{
  await syncSimulators();
  if(simulator.connectionError)throw new Error(simulator.connectionError);
  const other=simulators.find(item=>item!==simulator);
  if(enabled&&other.connectionError)throw new Error('暂时无法确认'+other.label+'的保留设置，请稍后重试：'+other.connectionError);
  if(confirmedState!==confirmationState(simulator,enabled))throw new Error('生成或保留设置已在其他页面更改，请关闭此窗口后重新确认。');
  simulator.state=await api(simulator.path,{method:'PUT',body:{enabled}});simulator.connectionError='';
  await syncSimulators();await refreshPage();
  toast(enabled?simulator.label+'已开启':simulator.label+'已关闭');
 }catch(e){await syncSimulators();throw e;}
 finally{changing=false;renderSimulatorState();}
}
async function setRetention(simulator,retainData){
 if(changing)return;
 changing=true;savingRetention=simulator;++statusVersion;renderSimulatorState();
 try{
  simulator.state=await api(simulator.path,{method:'PATCH',body:{retainData}});simulator.connectionError='';
  await syncSimulators();
  toast(simulator.label+'：已设置停止后'+(retainData?'保留':'清理')+'，历史保留数据不受影响');
 }catch(e){await syncSimulators();toast(errorText(e),true);}
 finally{changing=false;savingRetention=null;renderSimulatorState();}
}
function stopEffect(simulator){
 const records=simulator.kind==='data'?'本轮新增的用户、渠道关系和订单':'本轮新增的订单';
 return (simulator.state?.retainData?'保留':'清理')+records+'。';
}
for(const simulator of simulators){
 simulator.retainToggle.onclick=()=>{
  if(!simulator.state||simulator.connectionError||changing)return;
  void setRetention(simulator,!simulator.state.retainData);
 };
 simulator.toggle.onclick=()=>{
  if(!simulator.state||simulator.connectionError||changing)return;
  const enabled=!simulator.state.enabled,isData=simulator.kind==='data';
  const other=simulators.find(item=>item!==simulator);
  let message;
  if(enabled){
   message=(other.state?.enabled?'将停止'+other.label+'，'+stopEffect(other):'两种生成模式互斥。')+
    '当前设置：本次生成停止后将'+stopEffect(simulator)+
    (isData?'每 0.5 秒新增 1–3 单，每 15 秒新增 1–2 名用户。':'每 0.5 秒新增 1–3 单，使用现有用户。')+
    '运行期间可调整保留设置，以停止时最后保存的设置为准。任一模式运行时，会继续推进历史保留订单的未完成状态。关闭浏览器后仍继续运行。';
  }else message='将停止生成，按当前设置'+stopEffect(simulator)+'两种模式均关闭时，订单状态暂停流转。';
  message+='历史已保留的数据不受影响。';
  const confirmedState=confirmationState(simulator,enabled);
  confirmAction((enabled?'开启':'关闭')+simulator.label,message,()=>setSimulator(simulator,enabled,confirmedState));
 };
}
window.addEventListener('hashchange',route);
window.addEventListener('focus',poll);
await route();await poll();setInterval(poll,2000);
