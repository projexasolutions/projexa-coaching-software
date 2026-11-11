import {useCallback,useEffect,useState} from 'react';
import {Alert,Box,Button,Card,CardContent,MenuItem,Stack,TextField,Typography} from '@mui/material';
import {api} from '../../services';

type Invitation={id:string;email:string;role_code:string;expires_at:string;created_at:string};

export default function UserInvitationsPage(){
 const [email,setEmail]=useState(''),[role,setRole]=useState('TEACHER'),[busy,setBusy]=useState(false),[error,setError]=useState(''),[success,setSuccess]=useState('');
 const [invitations,setInvitations]=useState<Invitation[]>([]),[loadingList,setLoadingList]=useState(true),[actionId,setActionId]=useState('');
 const load=useCallback(async()=>{setLoadingList(true);try{const r=await api.get('/invitations');const data=Array.isArray(r.data)?r.data:(r.data?.data||[]);setInvitations(data);}catch(err:any){setError(err?.response?.data?.message??'Could not load pending invitations.');}finally{setLoadingList(false);}},[]);
 useEffect(()=>{void load();},[load]);
 const submit=async(e:any)=>{e.preventDefault();setBusy(true);setError('');setSuccess('');try{const r=await api.post('/invitations',{email,role});setSuccess(r.data?.message||'Invitation sent.');setEmail('');await load();}catch(err:any){setError(err?.response?.data?.message??err?.response?.data?.error?.message??'Could not send invitation.');}finally{setBusy(false);}};
 const resend=async(id:string)=>{setActionId(id);setError('');setSuccess('');try{const r=await api.post('/invitations/'+id+'/resend');setSuccess(r.data?.message||'Invitation link renewed.');await load();}catch(err:any){setError(err?.response?.data?.message??'Could not resend invitation.');}finally{setActionId('');}};
 const revoke=async(id:string)=>{setActionId(id);setError('');setSuccess('');try{await api.delete('/invitations/'+id);setSuccess('Invitation revoked.');await load();}catch(err:any){setError(err?.response?.data?.message??'Could not revoke invitation.');}finally{setActionId('');}};
 const date=(value:string)=>value?new Date(value).toLocaleString(): '—';
 return <Stack spacing={2.5} sx={{maxWidth:900}}>
  <Card elevation={0} sx={{border:'1px solid #e5eaf0',borderRadius:3}}><CardContent sx={{p:{xs:2.5,md:3.5}}}>
   <Typography sx={{fontSize:19,fontWeight:900}}>Invite an institute user</Typography>
   <Typography sx={{fontSize:13,color:'#64748b',mt:.6,mb:2.5}}>Invite an existing teacher, student or parent profile to activate their secure portal account.</Typography>
   {error&&<Alert severity="error" sx={{mb:2}}>{error}</Alert>}{success&&<Alert severity="success" sx={{mb:2}}>{success}</Alert>}
   <Box component="form" onSubmit={submit}><Stack spacing={2}>
    <TextField label="Profile email address" type="email" value={email} onChange={e=>setEmail(e.target.value)} required fullWidth/>
    <TextField select label="Account role" value={role} onChange={e=>setRole(e.target.value)} fullWidth>
     <MenuItem value="TEACHER">Teacher</MenuItem><MenuItem value="STUDENT">Student</MenuItem><MenuItem value="PARENT">Parent</MenuItem>
    </TextField>
    <Button type="submit" variant="contained" disabled={busy} sx={{alignSelf:{xs:'stretch',sm:'flex-start'},textTransform:'none',fontWeight:800,borderRadius:2}}>{busy?'Sending invitation…':'Send secure invitation'}</Button>
   </Stack></Box>
   <Alert severity="info" sx={{mt:2.5}}>The email must match exactly one existing profile in this institute. Invitations expire automatically and can only be accepted once. SMTP must be configured.</Alert>
  </CardContent></Card>
  <Box>
   <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{mb:1.5}}>
    <Box><Typography sx={{fontSize:18,fontWeight:900}}>Pending invitations</Typography><Typography sx={{fontSize:12,color:'#64748b'}}>Renew an expired link or revoke access before it is accepted.</Typography></Box>
    <Button onClick={()=>void load()} disabled={loadingList} size="small" sx={{textTransform:'none'}}>Refresh</Button>
   </Stack>
   {loadingList?<Typography sx={{color:'#64748b',fontSize:13}}>Loading invitations…</Typography>:invitations.length===0?<Card elevation={0} sx={{border:'1px dashed #cbd5e1',borderRadius:3}}><CardContent><Typography sx={{fontSize:13,color:'#64748b'}}>No pending invitations.</Typography></CardContent></Card>:<Stack spacing={1.25}>
    {invitations.map(item=><Card key={item.id} elevation={0} sx={{border:'1px solid #e5eaf0',borderRadius:2.5}}><CardContent sx={{p:2,'&:last-child':{pb:2}}}>
     <Stack direction={{xs:'column',sm:'row'}} spacing={1.5} alignItems={{sm:'center'}}>
      <Box sx={{flex:1,minWidth:0}}><Typography sx={{fontWeight:800,fontSize:14,overflowWrap:'anywhere'}}>{item.email}</Typography><Typography sx={{fontSize:11.5,color:'#64748b',mt:.35}}>{item.role_code} · Expires {date(item.expires_at)}</Typography></Box>
      <Stack direction="row" spacing={1}><Button size="small" variant="outlined" disabled={!!actionId} onClick={()=>void resend(item.id)} sx={{textTransform:'none'}}>{actionId===item.id?'Working…':'Resend'}</Button><Button size="small" color="error" disabled={!!actionId} onClick={()=>void revoke(item.id)} sx={{textTransform:'none'}}>Revoke</Button></Stack>
     </Stack>
    </CardContent></Card>)}
   </Stack>}
  </Box>
 </Stack>;
}
