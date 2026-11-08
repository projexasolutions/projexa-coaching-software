import {useState} from 'react';
import {Alert,Box,Button,Card,CardContent,MenuItem,Stack,TextField,Typography} from '@mui/material';
import {api} from '../../services';

export default function UserInvitationsPage(){
 const [email,setEmail]=useState(''),[role,setRole]=useState('TEACHER'),[busy,setBusy]=useState(false),[error,setError]=useState(''),[success,setSuccess]=useState('');
 const submit=async(e:any)=>{e.preventDefault();setBusy(true);setError('');setSuccess('');try{const r=await api.post('/invitations',{email,role});setSuccess(r.data?.message||'Invitation sent.');setEmail('');}catch(err:any){setError(err?.response?.data?.message??err?.response?.data?.error?.message??'Could not send invitation.');}finally{setBusy(false);}};
 return <Card elevation={0} sx={{border:'1px solid #e5eaf0',borderRadius:3,maxWidth:760}}><CardContent sx={{p:{xs:2.5,md:3.5}}}>
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
  <Alert severity="info" sx={{mt:2.5}}>The email must match exactly one existing profile in this institute. The invitation expires automatically and can only be accepted once. SMTP must be configured.</Alert>
 </CardContent></Card>;
}