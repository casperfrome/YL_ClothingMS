import {api,query} from './api.js';
import {esc,money,options,badge,swatch,image,empty,pagination,toast,showError,errorText,formModal,confirmAction,preview,field,heading} from './ui.js';
const sizes=['S','M','L','XL','XXL'];
function productForm(product,categories,onSave){
 formModal(product?'编辑商品':'新增商品',
 field('款号','<input name="code" required maxlength="40" value="'+esc(product?.code)+'" placeholder="例如 YL-2607">')+
 field('商品名称','<input name="name" required maxlength="100" value="'+esc(product?.name)+'" placeholder="输入商品名称">')+
 field('所属品类','<select name="categoryId" required>'+options(categories,product?.categoryId,'请选择品类')+'</select>',true)+
 field('商品描述','<textarea name="description" maxlength="10000" placeholder="面料、版型、穿着特点…">'+esc(product?.description)+'</textarea>',true),
 async data=>{await onSave({code:data.get('code').trim(),name:data.get('name').trim(),categoryId:Number(data.get('categoryId')),description:data.get('description')});});
}
function colorForm(color,onSave){
 formModal(color?'编辑配色':'新增配色',
 field('颜色名称','<input name="name" required maxlength="40" value="'+esc(color?.name)+'" placeholder="例如 红黑色">',true)+
 field('调色盘','<div class="color-picker"><input name="hex1" type="color" aria-label="第一种颜色" value="'+esc(color?.hex1||'#4956ce')+'"><label><input name="dual" type="checkbox" '+(color?.hex2?'checked':'')+'>双色组合</label><input name="hex2" type="color" aria-label="第二种颜色" value="'+esc(color?.hex2||'#222635')+'" '+(color?.hex2?'':'hidden')+'></div>',true)+
 (!color?field('各尺码、渠道的初始售价（元）','<input name="initialPrice" required type="number" min="0.01" max="99999999.99" step="0.01" placeholder="0.00">',true)+'<div class="help field full">自动创建 S–XXL 五种尺码和四个渠道，新建销售记录默认为下架。</div>':''),
 async data=>{const body={name:data.get('name').trim(),hex1:data.get('hex1'),hex2:data.has('dual')?data.get('hex2'):null};if(!color)body.initialPrice=data.get('initialPrice');await onSave(body);},
 form=>form.elements.dual.onchange=()=>form.elements.hex2.hidden=!form.elements.dual.checked);
}
export async function productList(root,meta){
 const state={page:1,pageSize:20,keyword:'',categoryId:'',channelId:'',onSale:''};
 async function render(){
  const data=await api('/products?'+query(state));
  root.innerHTML=heading('商品管理','从款式出发，管理每个颜色、尺码与销售渠道。','<button class="primary" data-create>＋ 新增商品</button>')+
  '<section class="panel"><form class="filters" id="product-filter">'+
  field('搜索商品','<input name="keyword" value="'+esc(state.keyword)+'" placeholder="搜索款号 / 商品名称" aria-label="搜索款号或商品名称">')+
  field('品类','<select name="categoryId">'+options(meta.categories,state.categoryId,'全部品类')+'</select>')+
  field('销售渠道','<select name="channelId">'+options(meta.channels,state.channelId,'全部渠道')+'</select>')+
  field('销售状态','<select name="onSale">'+options([{id:'true',name:'已上架'},{id:'false',name:'已下架'}],state.onSale,'全部状态')+'</select>')+
  '<button class="primary" type="submit">查询</button><button type="button" data-reset>重置</button></form>'+
  (data.items.length?'<div class="table-wrap"><table><thead><tr><th>商品 / 款号</th><th>品类</th><th>配色</th><th>渠道售价</th><th>销售状态</th><th>操作</th></tr></thead><tbody>'+data.items.map(p=>'<tr><td><a class="product-cell" href="#/products/'+p.id+'">'+image(p.thumbnail,'',p.name)+'<span><span class="product-name">'+esc(p.name)+'</span><span class="product-code mono">'+esc(p.code)+'</span></span></a></td><td><span class="category-label">'+esc(p.categoryName)+'</span></td><td>'+p.colorCount+' 色</td><td class="price nowrap">'+money(p.minPrice)+(p.minPrice!==p.maxPrice&&p.maxPrice?' <span class="muted">–</span> '+Number(p.maxPrice).toFixed(2):'')+'</td><td>'+badge(p.onSaleCount>0?(p.onSaleCount===p.listingCount?'全部上架':'部分上架'):'全部下架',p.onSaleCount>0?'green':'')+'<div class="help">'+p.onSaleCount+' / '+p.listingCount+' 条在售</div></td><td><a class="text-button nowrap" href="#/products/'+p.id+'">管理商品 →</a></td></tr>').join('')+'</tbody></table></div>':empty('没有找到商品','调整筛选条件，或新增一个款式。'))+pagination(data)+'</section>';
  root.querySelector('form').onsubmit=async e=>{e.preventDefault();Object.assign(state,Object.fromEntries(new FormData(e.target)),{page:1});try{await render();}catch(err){toast(errorText(err),true);}};
 }
 root.addEventListener('click',async e=>{
  const target=e.target.closest('button');if(!target)return;
  try{
   if(target.hasAttribute('data-create'))productForm(null,meta.categories,async body=>{const created=await api('/products',{method:'POST',body});location.hash='/products/'+created.id;});
   if(target.hasAttribute('data-reset')){Object.assign(state,{page:1,keyword:'',categoryId:'',channelId:'',onSale:''});await render();}
   if(target.dataset.page){state.page=Number(target.dataset.page);await render();}
  }catch(err){toast(errorText(err),true);}
 });
 await render();
}
export async function productDetail(root,id,meta){
 let product,currentColor=null,listings;let busy=false;
 const selected=new Set();const filter={page:1,pageSize:20,channelId:'',size:'',onSale:''};
 async function render(){
  product=await api('/products/'+id);
  if(!product.colors.some(c=>c.id===currentColor))currentColor=product.colors[0]?.id??null;
  const color=product.colors.find(c=>c.id===currentColor);
  listings=color?await api('/products/'+id+'/listings?'+query({...filter,colorId:currentColor})):{items:[],total:0,page:1,pageSize:20};
  selected.clear();
  root.innerHTML='<a href="#/products" class="back-link">← 返回商品列表</a><section class="panel product-summary">'+image(product.thumbnail,'summary-image',product.name)+
  '<div class="summary-body"><div class="summary-meta"><span class="mono muted">'+esc(product.code)+'</span><span class="category-label">'+esc(product.categoryName)+'</span></div><h1>'+esc(product.name)+'</h1><div class="description">'+esc(product.description||'尚未填写商品描述')+'</div></div><div class="heading-actions"><button data-action="edit-product">编辑商品</button><button class="text-button danger" data-action="delete-product">删除</button></div></section>'+
  '<div class="detail-grid"><section class="panel"><div class="panel-title"><h2>配色 <small>'+product.colors.length+' 色</small></h2><button class="text-button" data-action="add-color">＋ 新增</button></div><div class="colors-list">'+product.colors.map(c=>'<button class="color-card '+(currentColor===c.id?'active':'')+'" data-color="'+c.id+'">'+image(c.images[0]?.url,'',c.name)+'<span><strong>'+esc(c.name)+'</strong>'+swatch(c)+' <span class="muted" style="font-size:11px">'+(c.hex2?'双色':'单色')+' · '+c.images.length+' 张图片</span></span></button>').join('')+'</div>'+
  (color?'<div class="color-detail"><div class="heading-actions"><span class="swatch-label">'+swatch(color)+' '+esc(color.name)+'</span><span><button class="text-button small-button" data-action="edit-color">编辑</button><button class="text-button small-button danger" data-action="delete-color">删除</button></span></div><div class="gallery">'+color.images.map((img,i)=>'<div class="gallery-item"><button class="gallery-image" data-preview="'+esc(img.url)+'" aria-label="预览图片 '+(i+1)+'">'+image(img.url,'',color.name+'预览图 '+(i+1))+'</button><div class="gallery-actions"><button data-image-left="'+img.id+'" '+(i===0?'disabled':'')+' aria-label="图片前移">←</button><button data-delete-image="'+img.id+'" aria-label="删除图片 '+(i+1)+'">删除</button><button data-image-right="'+img.id+'" '+(i===color.images.length-1?'disabled':'')+' aria-label="图片后移">→</button></div></div>').join('')+'</div><label class="upload-label">＋ 添加预览图<input type="file" id="image-upload" accept="image/jpeg,image/png,image/webp" multiple></label><div class="help">JPG / PNG / WebP · 单张不超过 5MB<br>第一张图片作为配色封面。</div></div>':empty('添加第一个配色','选择颜色后自动创建尺码及渠道记录。'))+
  '</section><section class="panel"><div class="panel-title"><h2>尺码与渠道销售设置</h2><span class="muted" style="font-size:12px">'+(color?esc(color.name):'')+'</span></div>'+
  '<form class="filters" id="listing-filter">'+field('尺码','<select name="size">'+options(sizes.map(s=>({id:s,name:s})),filter.size,'全部尺码')+'</select>')+field('渠道','<select name="channelId">'+options(meta.channels,filter.channelId,'全部渠道')+'</select>')+field('状态','<select name="onSale">'+options([{id:'true',name:'已上架'},{id:'false',name:'已下架'}],filter.onSale,'全部状态')+'</select>')+'<button type="submit">筛选</button></form>'+
  '<div class="batchbar"><span id="selection-count">已选 0 条</span><button class="small-button" data-action="batch-price" data-needs-selection disabled>批量调价</button><button class="small-button" data-action="batch-on" data-needs-selection disabled>上架</button><button class="small-button" data-action="batch-off" data-needs-selection disabled>下架</button></div>'+
  (listings.items.length?'<div class="table-wrap"><table class="listing-table"><thead><tr><th><input type="checkbox" id="select-all" aria-label="选择本页全部销售记录"></th><th>尺码</th><th>渠道</th><th>售价（元）</th><th>状态</th><th>操作</th></tr></thead><tbody>'+listings.items.map(l=>'<tr data-row="'+l.id+'"><td><input type="checkbox" data-select="'+l.id+'" aria-label="选择 '+esc(l.channelName)+' '+l.size+'"></td><td><strong>'+l.size+'</strong></td><td>'+esc(l.channelName)+'</td><td><input type="number" data-price="'+l.id+'" aria-label="'+esc(l.channelName)+' '+l.size+' 售价" value="'+esc(l.price)+'" min="0.01" max="99999999.99" step="0.01" required></td><td>'+badge(l.onSale?'已上架':'已下架',l.onSale?'green':'')+'</td><td class="nowrap"><button class="text-button small-button" data-save="'+l.id+'">保存</button><button class="text-button small-button" data-toggle="'+l.id+'">'+(l.onSale?'下架':'上架')+'</button></td></tr>').join('')+'</tbody></table></div>':empty('暂无销售记录',color?'请调整筛选条件。':'请先添加配色。'))+pagination(listings)+'</section></div>';
  root.querySelector('#listing-filter').onsubmit=async e=>{e.preventDefault();Object.assign(filter,Object.fromEntries(new FormData(e.target)),{page:1});try{await render();}catch(err){toast(errorText(err),true);}};
 }
 function syncSelection(){root.querySelector('#selection-count').textContent='已选 '+selected.size+' 条';root.querySelectorAll('[data-needs-selection]').forEach(b=>b.disabled=!selected.size);const all=root.querySelector('#select-all');if(all){all.checked=selected.size===listings.items.length&&selected.size>0;all.indeterminate=selected.size>0&&!all.checked;}}
 root.addEventListener('change',async e=>{
  if(e.target.dataset.select){const sid=Number(e.target.dataset.select);e.target.checked?selected.add(sid):selected.delete(sid);syncSelection();}
  if(e.target.id==='select-all'){selected.clear();root.querySelectorAll('[data-select]').forEach(x=>{x.checked=e.target.checked;if(x.checked)selected.add(Number(x.dataset.select));});syncSelection();}
  if(e.target.id==='image-upload'&&e.target.files.length){e.target.disabled=true;try{const data=new FormData();for(const f of e.target.files)data.append('files',f);await api('/product-colors/'+currentColor+'/images',{method:'POST',body:data});await render();toast('图片已添加');}catch(err){toast(errorText(err),true);e.target.disabled=false;e.target.value='';}}
 });
 root.addEventListener('click',async e=>{
  const b=e.target.closest('button');if(!b||busy)return;
  const color=product.colors.find(c=>c.id===currentColor);
  try{
   if(b.dataset.color){currentColor=Number(b.dataset.color);filter.page=1;await render();}
   else if(b.dataset.page){filter.page=Number(b.dataset.page);await render();}
   else if(b.dataset.preview){preview(b.dataset.preview);}
   else if(b.dataset.save){const input=root.querySelector('[data-price="'+b.dataset.save+'"]');if(!input.reportValidity())return;b.disabled=true;await api('/listings/'+b.dataset.save,{method:'PATCH',body:{price:input.value}});await render();toast('价格已保存');}
   else if(b.dataset.toggle){const row=listings.items.find(l=>l.id===Number(b.dataset.toggle));await api('/listings/'+row.id,{method:'PATCH',body:{onSale:!row.onSale}});await render();toast(row.onSale?'已下架':'已上架');}
   else if(b.dataset.deleteImage){confirmAction('删除预览图','删除后无法恢复，确认删除这张图片？',async()=>{await api('/images/'+b.dataset.deleteImage,{method:'DELETE'});await render();toast('图片已删除');});}
   else if(b.dataset.imageLeft||b.dataset.imageRight){const ids=color.images.map(x=>x.id),pos=ids.indexOf(Number(b.dataset.imageLeft||b.dataset.imageRight)),next=pos+(b.dataset.imageLeft?-1:1);[ids[pos],ids[next]]=[ids[next],ids[pos]];await api('/product-colors/'+currentColor+'/images/order',{method:'PUT',body:{ids}});await render();}
   else switch(b.dataset.action){
    case 'edit-product':productForm(product,meta.categories,async body=>{await api('/products/'+id,{method:'PATCH',body});await render();});break;
    case 'delete-product':confirmAction('删除商品','该款式及其所有销售记录将停止销售，已有订单仍可查询。确认删除？',async()=>{await api('/products/'+id,{method:'DELETE'});location.hash='/products';toast('商品已删除');});break;
    case 'add-color':colorForm(null,async body=>{const c=await api('/products/'+id+'/colors',{method:'POST',body});currentColor=c.id;filter.page=1;await render();});break;
    case 'edit-color':colorForm(color,async body=>{await api('/product-colors/'+currentColor,{method:'PATCH',body});await render();});break;
    case 'delete-color':confirmAction('删除配色','该配色下的所有尺码和渠道记录将停止销售，已有订单仍可查询。',async()=>{await api('/product-colors/'+currentColor,{method:'DELETE'});currentColor=null;filter.page=1;await render();toast('配色已删除');});break;
    case 'batch-price':formModal('批量调整售价',field('将所选 '+selected.size+' 条销售记录的售价设为（元）','<input type="number" name="price" min="0.01" max="99999999.99" step="0.01" required placeholder="0.00">',true),async data=>{await api('/listings',{method:'PATCH',body:{ids:[...selected],price:data.get('price')}});await render();});break;
    case 'batch-on':case 'batch-off':await api('/listings',{method:'PATCH',body:{ids:[...selected],onSale:b.dataset.action==='batch-on'}});await render();toast('销售状态已更新');break;
   }
  }catch(err){toast(errorText(err),true);b.disabled=false;}
 });
 await render();
}

