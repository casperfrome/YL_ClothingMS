export async function api(path, options={}) {
  const init={...options,headers:{...options.headers},signal:options.signal||AbortSignal.timeout(15000)};
  if(init.body && !(init.body instanceof FormData)){init.headers['Content-Type']='application/json';init.body=JSON.stringify(init.body);}
  const response=await fetch('/api'+path,init);
  if(response.status===204)return null;
  const body=await response.json().catch(()=>({message:'服务返回了无法识别的响应'}));
  if(!response.ok){const e=new Error(body.message||'请求失败');e.fields=body.fieldErrors||{};e.status=response.status;throw e;}
  return body.data;
}
export function query(values){return new URLSearchParams(Object.entries(values).filter(([,v])=>v!==''&&v!==null&&v!==undefined)).toString();}

