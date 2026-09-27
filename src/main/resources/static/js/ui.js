export const esc = v => String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export const money = v => v==null?'—':'¥ '+Number(v).toFixed(2);
export const date = v => v?new Intl.DateTimeFormat('zh-CN',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',second:'2-digit',hour12:false}).format(new Date(v)):'—';
export const statusNames={PENDING_PAYMENT:'待付款',CANCELLED:'已取消',PENDING_SHIPMENT:'待发货',SHIPPED:'已发货',REFUNDED:'已退款',COMPLETED:'已完成'};
export const genderNames={UNKNOWN:'未设置',MALE:'男',FEMALE:'女'};
export const options=(items,selected,all='')=>(all?'<option value="">'+esc(all)+'</option>':'')+items.map(x=>'<option value="'+esc(x.id)+'" '+(String(selected)===String(x.id)?'selected':'')+'>'+esc(x.name)+'</option>').join('');
export const badge=(text,type='')=>'<span class="badge '+type+'">'+esc(text)+'</span>';
export function orderBadge(status){return badge(statusNames[status]||status,({PENDING_PAYMENT:'orange',PENDING_SHIPMENT:'blue',SHIPPED:'blue',COMPLETED:'green',REFUNDED:'red'})[status]||'');}
export const swatch=c=>'<span class="swatch" style="background:'+ (c.hex2?'linear-gradient(135deg,'+esc(c.hex1)+' 50%,'+esc(c.hex2)+' 50%)':esc(c.hex1))+'"></span>';
export const image=(url,cls='',alt='商品预览')=>url?'<img class="thumb '+cls+'" src="'+esc(url)+'" alt="'+esc(alt)+'" loading="lazy">':'<span class="thumb '+cls+'" aria-label="暂无图片" style="display:grid;place-items:center;color:#a6adbc;font-size:22px">▧</span>';
export function empty(title='暂无数据',description='调整筛选条件后再试试。'){return '<div class="empty"><strong>'+esc(title)+'</strong>'+esc(description)+'</div>';}
export function pagination(data){const pages=Math.max(1,Math.ceil(data.total/data.pageSize));return '<div class="pagination"><span>共 '+data.total+' 条记录</span><div class="pages"><button data-page="'+(data.page-1)+'" '+(data.page<=1?'disabled':'')+' aria-label="上一页">‹</button><span>'+data.page+' / '+pages+'</span><button data-page="'+(data.page+1)+'" '+(data.page>=pages?'disabled':'')+' aria-label="下一页">›</button></div></div>';}
let toastTimer;
export function toast(message,error=false){const el=document.querySelector('#toast');el.textContent=message;el.className='toast'+(error?' error':'');el.hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(()=>el.hidden=true,3500);}
export function errorText(e){return e.name==='TimeoutError'?'连接超时，请检查后端服务。':e.message==='Failed to fetch'?'无法连接后端，请确认服务已启动。':e.message;}
export function showError(container,e){container.innerHTML=empty('加载失败',errorText(e))+'<div style="text-align:center;margin-bottom:24px"><button data-retry>重新加载</button></div>';container.querySelector('[data-retry]').onclick=()=>window.dispatchEvent(new HashChangeEvent('hashchange'));}
const modal=document.querySelector('#modal');
export function closeModal(){modal.close();modal.innerHTML='';delete modal.dataset.orderId;}
function modalShell(title,body,actions){delete modal.dataset.orderId;if(modal.open)modal.close();modal.innerHTML='<div class="modal-head"><h2>'+esc(title)+'</h2><button type="button" class="close-button" aria-label="关闭">×</button></div>'+body+actions;modal.querySelector('.close-button').onclick=closeModal;modal.showModal();}
export function formModal(title,fields,onSave,afterOpen){
 modalShell(title,'<form id="dialog-form"><div class="modal-body"><div class="form-error" role="alert"></div><div class="form-grid">'+fields+'</div></div><div class="modal-actions"><button type="button" data-cancel>取消</button><button class="primary" type="submit">保存</button></div></form>','');
 modal.querySelector('[data-cancel]').onclick=closeModal;
 const form=modal.querySelector('form');
 form.onsubmit=async e=>{e.preventDefault();const button=form.querySelector('[type=submit]');button.disabled=true;const errors=form.querySelector('.form-error');errors.textContent='';try{if(await onSave(new FormData(form),form)===false)return;if(form.isConnected)closeModal();toast('已保存');}catch(err){errors.textContent=errorText(err)+(Object.values(err.fields||{}).length?'：'+Object.values(err.fields).join('；'):'');}finally{button.disabled=false;}};
 afterOpen?.(form);return form;
}
export function confirmAction(title,message,onConfirm){
 modalShell(title,'<div class="modal-body"><div class="form-error"></div><p style="line-height:1.8;margin:0">'+esc(message)+'</p></div>','<div class="modal-actions"><button data-cancel>取消</button><button class="primary" data-confirm>确认</button></div>');
 modal.querySelector('[data-cancel]').onclick=closeModal;
 modal.querySelector('[data-confirm]').onclick=async e=>{const button=e.target,errors=modal.querySelector('.form-error');button.disabled=true;try{await onConfirm();if(button.isConnected)closeModal();}catch(err){if(button.isConnected)errors.textContent=errorText(err);else toast(errorText(err),true);button.disabled=false;}};
}
export function preview(url){modalShell('商品预览','<img class="large-preview" src="'+esc(url)+'" alt="商品大图">','');}
export function detailModal(title,body){modalShell(title,'<div class="modal-body">'+body+'</div>','');return modal;}
export function field(label,control,full=false){return '<label class="field'+(full?' full':'')+'"><span>'+esc(label)+'</span>'+control+'</label>';}
export function heading(title,subtitle,action=''){return '<div class="page-heading"><div><h1>'+esc(title)+'</h1><p>'+esc(subtitle)+'</p></div>'+action+'</div>';}



