import {useState} from 'react';
import {Alert,Box,Button,Card,CardContent,TextField,Typography} from '@mui/material';
import {Link as RouterLink,useSearchParams} from 'react-router-dom';
import {api} from '../../services';

export default function PasswordResetPage(){
 const [params]=useSearchParams();
 const token=params.get('token')||'';
 const [tenantSlug,setTenantSlug]=useState('demo');
 const [email,setEmail]=useState('');
 const [password,setPassword]=useState('');
 const [message,setMessage]=useState('');
 const [error,setError]=useState('');
 const [loading,setLoading]=useState(false);
 const request=async(e:any)=>{e.preventDefault();setLoading(true);setError('');setMessage('');try{const r=await api.post('/auth/password-reset/request',{tenantSlug,email});setMessage(r.data?.message||'If an active account matches those details, a reset link will be sent.')}catch(err:any){setError(err?.response?.data?.message||'Unable to request a reset right now.')}finally{setLoading(false)}};
 const confirm=async(e:any)=>{e.preventDefault();setLoading(true);setError('');setMessage('');try{const r=await api.post('/auth/password-reset/confirm',{token,password});setMessage(r.data?.message||'Password updated. You can sign in now.')}catch(err:any){setError(err?.response?.data?.message||'This reset link is invalid or expired.')}finally{setLoading(false)}};
 return <Box sx={{minHeight:'100vh',display:'grid',placeItems:'center',bgcolor:'#f5f8f7',p:2}}>
  <Card elevation={0} sx={{width:'100%',maxWidth:460,border:'1px solid #e5e7eb',borderRadius:4}}>
   <CardContent sx={{p:{xs:3,sm:4}}}>
    <Typography sx={{fontWeight:950,fontSize:16,letterSpacing:-.3}}>PROJEXA COACHING OS</Typography>
    <Typography component="h1" sx={{fontSize:26,fontWeight:900,mt:2}}>{token?'Choose a new password':'Reset your password'}</Typography>
    <Typography sx={{fontSize:13,color:'#64748b',mt:.5,mb:3}}>{token?'Use a strong password with at least 12 characters.':'Enter your institute slug and account email. If the account exists, we will email a reset link.'}</Typography>
    {error&&<Alert severity="error" sx={{mb:2}}>{error}</Alert>}
    {message&&<Alert severity="success" sx={{mb:2}}>{message}</Alert>}
    <Box component="form" onSubmit={token?confirm:request} sx={{display:'grid',gap:2}}>
     {!token&&<><TextField label="Institute slug" value={tenantSlug} onChange={e=>setTenantSlug(e.target.value)} required size="small"/><TextField label="Email" type="email" value={email} onChange={e=>setEmail(e.target.value)} required size="small"/></>}
     {token&&<TextField label="New password" type="password" value={password} onChange={e=>setPassword(e.target.value)} required inputProps={{minLength:12,maxLength:72}} size="small"/>}
     <Button type="submit" variant="contained" disabled={loading} sx={{textTransform:'none',fontWeight:800,borderRadius:2}}>{loading?'Please wait…':token?'Update password':'Send reset link'}</Button>
    </Box>
    <Button component={RouterLink} to="/login" fullWidth sx={{mt:1.5,textTransform:'none'}}>Back to sign in</Button>
   </CardContent>
  </Card>
 </Box>;
}
