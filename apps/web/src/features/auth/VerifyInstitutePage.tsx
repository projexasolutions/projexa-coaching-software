import {useEffect,useState} from 'react';
import {Alert,Box,Button,Card,CardContent,Typography} from '@mui/material';
import {Link as RouterLink,useSearchParams} from 'react-router-dom';
import {api} from '../../services';

export default function VerifyInstitutePage(){
 const [params]=useSearchParams();const token=params.get('token')||'';
 const [state,setState]=useState<'loading'|'success'|'error'>('loading');const [message,setMessage]=useState('Verifying your institute account…');
 useEffect(()=>{let active=true;if(!token){setState('error');setMessage('Verification link is invalid or missing.');return;}
 api.post('/auth/verify-institute',{token}).then(r=>{if(active){setState('success');setMessage(r.data?.message||'Institute verified. You can now sign in.');}}).catch(err=>{if(active){setState('error');setMessage(err?.response?.data?.message??err?.response?.data?.error?.message??'Verification link is invalid or expired.');}});
 return()=>{active=false;};},[token]);
 return <Box sx={{minHeight:'100vh',display:'grid',placeItems:'center',bgcolor:'#f5f8f7',p:2}}>
  <Card elevation={0} sx={{width:'100%',maxWidth:460,border:'1px solid #e5e7eb',borderRadius:4}}><CardContent sx={{p:4}}>
   <Typography sx={{fontWeight:950,fontSize:15,color:'#0f766e'}}>PROJEXA COACHING OS</Typography>
   <Typography component="h1" sx={{fontSize:26,fontWeight:900,mt:2,mb:2}}>Email verification</Typography>
   <Alert severity={state==='success'?'success':state==='error'?'error':'info'}>{message}</Alert>
   {state!=='loading'&&<Button component={RouterLink} to="/login" variant="contained" fullWidth sx={{mt:2.5,textTransform:'none',fontWeight:800}}>Go to sign in</Button>}
  </CardContent></Card>
 </Box>;
}