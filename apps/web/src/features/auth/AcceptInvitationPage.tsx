import {useState} from 'react';
import {Alert,Box,Button,Card,CardContent,TextField,Typography} from '@mui/material';
import {Link as RouterLink,useSearchParams} from 'react-router-dom';
import {api} from '../../services';

export default function AcceptInvitationPage(){
 const [params]=useSearchParams();const token=params.get('token')||'';
 const [password,setPassword]=useState(''),[confirm,setConfirm]=useState(''),[busy,setBusy]=useState(false),[error,setError]=useState(''),[done,setDone]=useState(false);
 const submit=async(e:any)=>{e.preventDefault();setError('');if(password!==confirm){setError('Passwords do not match.');return;}setBusy(true);try{await api.post('/invitations/accept',{token,password});setDone(true);}catch(err:any){setError(err?.response?.data?.message??err?.response?.data?.error?.message??'Invitation is invalid or expired.');}finally{setBusy(false);}};
 return <Box sx={{minHeight:'100vh',display:'grid',placeItems:'center',bgcolor:'#f5f8f7',p:2}}>
  <Card elevation={0} sx={{width:'100%',maxWidth:460,border:'1px solid #e5e7eb',borderRadius:4}}><CardContent sx={{p:4}}>
   <Typography sx={{fontWeight:950,fontSize:15,color:'#0f766e'}}>PROJEXA COACHING OS</Typography>
   <Typography component="h1" sx={{fontSize:26,fontWeight:900,mt:2,mb:1}}>Accept your invitation</Typography>
   {done?<><Alert severity="success">Invitation accepted. Your account is ready. You can now sign in using your institute slug and email.</Alert><Button component={RouterLink} to="/login" variant="contained" fullWidth sx={{mt:2,textTransform:'none'}}>Go to sign in</Button></>:
    <Box component="form" onSubmit={submit} sx={{display:'grid',gap:2,mt:2}}>
     {error&&<Alert severity="error">{error}</Alert>}
     {!token&&<Alert severity="error">Invitation token is missing.</Alert>}
     <TextField label="Create password" type="password" value={password} onChange={e=>setPassword(e.target.value)} required inputProps={{minLength:12,maxLength:72}} helperText="Use 12–72 characters"/>
     <TextField label="Confirm password" type="password" value={confirm} onChange={e=>setConfirm(e.target.value)} required/>
     <Button type="submit" variant="contained" disabled={busy||!token} sx={{textTransform:'none',fontWeight:800}}>{busy?'Activating account…':'Accept invitation'}</Button>
    </Box>}
  </CardContent></Card>
 </Box>;
}