import {useEffect,useState} from 'react';
import {Alert,Button,Card,CardContent,Chip,LinearProgress,Stack,Typography} from '@mui/material';
import {ArrowBackRounded} from '@mui/icons-material';
import {useNavigate,useParams} from 'react-router-dom';
import {api} from '../services';

export default function PlatformInstituteSetupPage(){
 const {id}=useParams(),nav=useNavigate(); const[data,setData]=useState<any>(null),[error,setError]=useState('');
 useEffect(()=>{api.get('/platform/institutes').then(r=>{const rows=r?.data?.data??r?.data??[];setData(rows.find((x:any)=>x.id===id)||null)}).catch((e:any)=>setError(e?.response?.data?.message||'Unable to load institute.'))},[id]);
 if(error)return <Alert severity="error">{error}</Alert>;
 if(!data)return <LinearProgress/>;
 return <Stack spacing={2.5}>
  <Button startIcon={<ArrowBackRounded/>} onClick={()=>nav('/admin/institutes')} sx={{alignSelf:'flex-start'}}>Back to institutes</Button>
  <Card variant="outlined" sx={{borderRadius:3}}><CardContent>
   <Stack direction="row" justifyContent="space-between"><div><Typography sx={{fontSize:22,fontWeight:950}}>{data.name}</Typography><Typography sx={{color:'#64748b',fontSize:12}}>{data.slug}</Typography></div><Chip label={(data.setup_status||'DRAFT').replaceAll('_',' ')}/></Stack>
   <Typography sx={{mt:2,color:'#64748b'}}>Institute setup workspace is connected to the platform institute record. Use the tenant Setup Center after switching into the institute context.</Typography>
   <Stack direction="row" spacing={2} sx={{mt:2}}><Typography><b>{data.programs}</b> programs</Typography><Typography><b>{data.batches}</b> batches</Typography><Typography><b>{data.students}</b> students</Typography></Stack>
   <Button sx={{mt:2}} variant="contained" href="/owner/setup">Open current tenant Setup Center</Button>
  </CardContent></Card>
 </Stack>
}