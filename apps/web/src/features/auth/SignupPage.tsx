import {useState} from 'react';
import {Alert,Box,Button,Card,CardContent,Link,Stack,TextField,Typography} from '@mui/material';
import {Link as RouterLink} from 'react-router-dom';
import {api} from '../../services';

export default function SignupPage(){
 const [form,setForm]=useState({instituteName:'',instituteSlug:'',firstName:'',lastName:'',email:'',password:''});
 const [loading,setLoading]=useState(false),[error,setError]=useState(''),[sent,setSent]=useState(false);
 const change=(key:string)=>(e:any)=>setForm(v=>({...v,[key]:e.target.value}));
 const submit=async(e:any)=>{e.preventDefault();setError('');setLoading(true);try{await api.post('/auth/signup',form);setSent(true);}catch(err:any){setError(err?.response?.data?.message??err?.response?.data?.error?.message??'Unable to submit signup. Please try again.');}finally{setLoading(false);}};
 return <Box sx={{minHeight:'100vh',display:'grid',placeItems:'center',bgcolor:'#f5f8f7',p:2}}>
  <Card elevation={0} sx={{width:'100%',maxWidth:560,border:'1px solid #e5e7eb',borderRadius:4}}>
   <CardContent sx={{p:{xs:3,sm:4}}}>
    <Typography sx={{fontWeight:950,fontSize:16,letterSpacing:.3,color:'#0f766e'}}>PROJEXA COACHING OS</Typography>
    <Typography component="h1" sx={{fontSize:27,fontWeight:900,mt:2}}>Create your institute account</Typography>
    <Typography sx={{fontSize:13,color:'#64748b',mt:.5,mb:2.5}}>Set up your institute workspace. You’ll need to verify your email before signing in.</Typography>
    {sent?<Alert severity="success">Signup received. Check your inbox for the verification link. The institute will activate only after email verification.</Alert>:
    <Box component="form" onSubmit={submit}>
     {error&&<Alert severity="error" sx={{mb:2}}>{error}</Alert>}
     <Stack spacing={1.7}>
      <TextField label="Institute name" value={form.instituteName} onChange={change('instituteName')} required fullWidth/>
      <TextField label="Institute URL slug" value={form.instituteSlug} onChange={change('instituteSlug')} required helperText="3–60 lowercase letters, numbers or hyphens"/>
      <Stack direction={{xs:'column',sm:'row'}} spacing={1.5}>
       <TextField label="Owner first name" value={form.firstName} onChange={change('firstName')} required fullWidth/>
       <TextField label="Last name" value={form.lastName} onChange={change('lastName')} fullWidth/>
      </Stack>
      <TextField label="Owner email" type="email" value={form.email} onChange={change('email')} required fullWidth/>
      <TextField label="Password" type="password" value={form.password} onChange={change('password')} required fullWidth helperText="Use at least 12 characters"/>
      <Button type="submit" variant="contained" size="large" disabled={loading} sx={{borderRadius:2,textTransform:'none',fontWeight:850}}>{loading?'Creating account…':'Create institute account'}</Button>
     </Stack>
    </Box>}
    <Typography sx={{fontSize:12,color:'#64748b',mt:2.5}}>Already registered? <Link component={RouterLink} to="/login" underline="hover">Sign in</Link></Typography>
   </CardContent>
  </Card>
 </Box>;
}