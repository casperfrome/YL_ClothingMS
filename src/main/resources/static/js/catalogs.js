import {api} from './api.js';
import {esc,heading,empty,formModal,confirmAction,field,toast,errorText} from './ui.js';
export async function categoriesPage(root){
 let categories=[];
 async function render(){
  categories=await api('/categories');
  root.innerHTML='<div class="category-page">'+heading('品类管理','清晰归类每个款式，保持商品目录井然有序。','<button class="primary" data-create>＋ 新增品类</button>')+'<section class="panel"><div class="panel-title"><h2>全部品类 <small>'+categories.length+' 个</small></h2></div>'+
  (categories.length?'<div class="table-wrap"><table><thead><tr><th>品类名称</th><th>关联商品</th><th>操作</th></tr></thead><tbody>'+categories.map(c=>'<tr><td><strong>'+esc(c.name)+'</strong></td><td>'+c.productCount+' 款</td><td><button class="text-button" data-edit="'+c.id+'">编辑</button><button class="text-button danger" data-delete="'+c.id+'">删除</button></td></tr>').join('')+'</tbody></table></div>':empty('尚未创建品类','新增品类后，就可以为商品进行分类。'))+'</section><p class="help">每个商品统一使用一个品类。仍被商品使用的品类无法删除。</p></div>';
 }
 root.addEventListener('click',e=>{
  const b=e.target.closest('button');if(!b)return;
  const category=categories.find(c=>c.id===Number(b.dataset.edit||b.dataset.delete));
  if(b.hasAttribute('data-create')||b.dataset.edit){
   formModal(category?'编辑品类':'新增品类',field('品类名称','<input name="name" required maxlength="60" value="'+esc(category?.name)+'" placeholder="例如 针织衫">',true),async data=>{await api('/categories'+(category?'/'+category.id:''),{method:category?'PATCH':'POST',body:{name:data.get('name').trim()}});await render();});
  }
  if(b.dataset.delete)confirmAction('删除品类','确认删除“'+category.name+'”？仍被商品使用的品类无法删除。',async()=>{await api('/categories/'+category.id,{method:'DELETE'});await render();toast('品类已删除');});
 });
 await render();
}
export async function channelsPage(root){
 const data=await api('/channels');
 root.innerHTML=heading('渠道管理','四个本地业务渠道，集中查看商品与用户的关联情况。')+'<div class="notice">渠道为本地演示数据，固定展示抖音、淘宝、京东、拼多多。用户开通关系来自初始数据和模拟数据生成；用户与订单数量自动同步。</div><div class="channel-grid">'+data.map(c=>'<section class="panel channel-card"><div class="channel-head"><div class="channel-icon '+c.code.toLowerCase()+'">'+esc(({DOUYIN:'抖',TAOBAO:'淘',JD:'京',PDD:'拼'})[c.code])+'</div><div><h2>'+esc(c.name)+'</h2><p>本地业务渠道</p></div></div><dl class="channel-stats"><div><dt>关联商品</dt><dd>'+c.productCount+'</dd></div><div><dt>已开通用户</dt><dd>'+c.userCount+'</dd></div><div><dt>模拟订单</dt><dd>'+c.orderCount+'</dd></div></dl><div class="channel-foot">'+c.onSaleCount+' 条「款 + 色 + 码」销售记录在售</div></section>').join('')+'</div>';
}


