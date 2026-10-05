import {useEffect,useMemo,useState} from 'react';
import {
 Alert,Button,Card,CardContent,Chip,Divider,LinearProgress,MenuItem,
 Stack,TextField,Typography
} from '@mui/material';
import {ArrowBackRounded,SaveRounded,PlayArrowRounded} from '@mui/icons-material';
import {useNavigate,useParams} from 'react-router-dom';
import {api} from '../services';

const STEPS=[
 ['INSTITUTE_PROFILE','Institute Profile'],
 ['ACADEMIC_STRUCTURE','Academic Structure'],
 ['PROGRAMS_EXAMS','Programs & Exams'],
 ['SUBJECTS','Subjects'],
 ['BATCHES','Batches'],
 ['FACULTY','Faculty'],
 ['FEE_PLANS','Fee Plans'],
 ['DATA_IMPORT','Data Import'],
 ['REVIEW_GO_LIVE','Review & Go Live']
] as const;

const emptyForm={
 instituteName:'',slug:'',instituteType:'OTHER',timezone:'Asia/Kolkata',
 logoUrl:'',primaryColor:'',secondaryColor:'',contactEmail:'',contactPhone:'',address:'',notes:''
};

function completedList(value:any):string[]{
 if(Array.isArray(value)) return value;
 if(typeof value==='string'){try{const parsed=JSON.parse(value);return Array.isArray(parsed)?parsed:[]}catch{return []}}
 return [];
}

export default function PlatformInstituteSetupPage(){
 const{id}=useParams(),nav=useNavigate();
 const[data,setData]=useState<any>(null),[form,setForm]=useState<any>(emptyForm);
 const[status,setStatus]=useState('DRAFT'),[step,setStep]=useState('INSTITUTE_PROFILE');
 const[completed,setCompleted]=useState<string[]>([]),[error,setError]=useState(''),[saving,setSaving]=useState(false);

 async function load(){
  if(!id)return;
  try{
   setError('');
   const r=await api.get('/platform/institutes/'+id+'/setup');
   const payload=r?.data?.data??r?.data;
   setData(payload);
   const p=payload.profile||{};
   setForm({
    instituteName:p.institute_name??'',slug:p.slug??'',instituteType:p.institute_type??'OTHER',
    timezone:p.timezone??'Asia/Kolkata',logoUrl:p.logo_url??'',primaryColor:p.primary_color??'',
    secondaryColor:p.secondary_color??'',contactEmail:p.contact_email??'',
    contactPhone:p.contact_phone??'',address:p.address??'',notes:p.notes??''
   });
   setStatus(p.setup_status??'DRAFT');
   setStep(p.current_step??'INSTITUTE_PROFILE');
   setCompleted(completedList(p.completed_steps));
  }catch(e:any){setError(e?.response?.data?.message||'Unable to load institute setup.')}
 }
 useEffect(()=>{load()},[id]);

 const progress=useMemo(()=>Math.round((completed.length/STEPS.length)*100),[completed]);
 function update(k:string,v:string){setForm((x:any)=>({...x,[k]:v}))}

 async function saveProfile(){
  if(!id)return;
  try{
   setSaving(true);setError('');
   await api.put('/platform/institutes/'+id+'/setup/profile',form);
   await load();
  }catch(e:any){setError(e?.response?.data?.message||'Unable to save institute profile.')}
  finally{setSaving(false)}
 }

 async function saveProgress(nextStatus=status,nextStep=step,nextCompleted=completed){
  if(!id)return;
  try{
   setSaving(true);setError('');
   await api.put('/platform/institutes/'+id+'/setup/progress',{
    currentStep:nextStep,status:nextStatus,completedSteps:JSON.stringify(nextCompleted),notes:form.notes||null
   });
   await load();
  }catch(e:any){setError(e?.response?.data?.message||'Unable to update setup progress.')}
  finally{setSaving(false)}
 }

 async function saveAndComplete(){
  const nextCompleted=completed.includes(step)?completed:[...completed,step];
  const nextIndex=Math.min(STEPS.findIndex(x=>x[0]===step)+1,STEPS.length-1);
  const nextStep=STEPS[nextIndex][0];
  const nextStatus=nextCompleted.length===STEPS.length?'READY_FOR_REVIEW':'CONFIGURING';
  await saveProgress(nextStatus,nextStep,nextCompleted);
 }

 async function goLive(){
  const nextCompleted=completed.includes('REVIEW_GO_LIVE')?completed:[...completed,'REVIEW_GO_LIVE'];
  await saveProgress('GO_LIVE','REVIEW_GO_LIVE',nextCompleted);
 }

 if(!id)return <Alert severity="error">Institute id is missing.</Alert>;
 if(!data&&!error)return <LinearProgress/>;
 const counts=data?.counts||{};

 return <Stack spacing={2.5}>
  <Button startIcon={<ArrowBackRounded/>} onClick={()=>nav('/admin/institutes')} sx={{alignSelf:'flex-start'}}>Back to institutes</Button>
  {error&&<Alert severity="error" onClose={()=>setError('')}>{error}</Alert>}

  <Card variant="outlined" sx={{borderRadius:3}}>
   <CardContent>
    <Stack direction={{xs:'column',md:'row'}} justifyContent="space-between" spacing={2}>
     <div>
      <Typography sx={{fontSize:24,fontWeight:950}}>{form.instituteName||'Institute Setup'}</Typography>
      <Typography sx={{color:'#64748b',fontSize:13}}>Platform onboarding workspace • {form.slug||'no-slug'}</Typography>
     </div>
     <Stack direction="row" spacing={1} alignItems="center">
      <Chip label={status.replaceAll('_',' ')} color={status==='GO_LIVE'?'success':status==='READY_FOR_REVIEW'?'warning':'default'}/>
      <Chip label={progress+'% complete'} variant="outlined"/>
     </Stack>
    </Stack>
    <LinearProgress variant="determinate" value={progress} sx={{mt:2,height:8,borderRadius:4}}/>
   </CardContent>
  </Card>

  <Card variant="outlined" sx={{borderRadius:3}}>
   <CardContent>
    <Typography sx={{fontWeight:900,fontSize:17}}>Onboarding steps</Typography>
    <Stack direction="row" flexWrap="wrap" gap={1} sx={{mt:1.5}}>
     {STEPS.map(([key,label],index)=>{
      const done=completed.includes(key);
      const active=step===key;
      return <Button key={key} size="small" variant={active?'contained':'outlined'} color={done?'success':'primary'}
       onClick={()=>setStep(key)} sx={{textTransform:'none'}}>
       {index+1}. {label}{done?' ✓':''}
      </Button>
     })}
    </Stack>
   </CardContent>
  </Card>

  {step==='INSTITUTE_PROFILE' ? <Card variant="outlined" sx={{borderRadius:3}}>
   <CardContent>
    <Typography sx={{fontWeight:900,fontSize:18}}>Institute profile</Typography>
    <Typography sx={{color:'#64748b',fontSize:13,mb:2}}>Configure the institute identity and contact information from the Projexa platform.</Typography>
    <Stack spacing={2}>
     <Stack direction={{xs:'column',md:'row'}} spacing={2}>
      <TextField label="Institute name" fullWidth value={form.instituteName} onChange={e=>update('instituteName',e.target.value)}/>
      <TextField label="Slug" fullWidth value={form.slug} onChange={e=>update('slug',e.target.value.toLowerCase())}/>
     </Stack>
     <Stack direction={{xs:'column',md:'row'}} spacing={2}>
      <TextField select label="Institute type" fullWidth value={form.instituteType} onChange={e=>update('instituteType',e.target.value)}>
       <MenuItem value="SCHOOL_COACHING">School Coaching</MenuItem><MenuItem value="COMPETITIVE_COACHING">Competitive Coaching</MenuItem>
       <MenuItem value="MULTI_EXAM">Multi Exam</MenuItem><MenuItem value="HYBRID">Hybrid</MenuItem><MenuItem value="OTHER">Other</MenuItem>
      </TextField>
      <TextField label="Timezone" fullWidth value={form.timezone} onChange={e=>update('timezone',e.target.value)}/>
     </Stack>
     <Stack direction={{xs:'column',md:'row'}} spacing={2}>
      <TextField label="Contact email" fullWidth value={form.contactEmail} onChange={e=>update('contactEmail',e.target.value)}/>
      <TextField label="Contact phone" fullWidth value={form.contactPhone} onChange={e=>update('contactPhone',e.target.value)}/>
     </Stack>
     <TextField label="Address" fullWidth multiline minRows={2} value={form.address} onChange={e=>update('address',e.target.value)}/>
     <Stack direction={{xs:'column',md:'row'}} spacing={2}>
      <TextField label="Logo URL" fullWidth value={form.logoUrl} onChange={e=>update('logoUrl',e.target.value)}/>
      <TextField label="Primary color" fullWidth value={form.primaryColor} onChange={e=>update('primaryColor',e.target.value)}/>
      <TextField label="Secondary color" fullWidth value={form.secondaryColor} onChange={e=>update('secondaryColor',e.target.value)}/>
     </Stack>
     <TextField label="Internal setup notes" fullWidth multiline minRows={3} value={form.notes} onChange={e=>update('notes',e.target.value)}/>
     <Stack direction="row" spacing={1}>
      <Button variant="contained" startIcon={<SaveRounded/>} disabled={saving} onClick={saveProfile}>Save profile</Button>
      <Button variant="outlined" disabled={saving} onClick={saveAndComplete}>Save & complete step</Button>
     </Stack>
    </Stack>
   </CardContent>
  </Card> : <Card variant="outlined" sx={{borderRadius:3}}>
   <CardContent>
    <Typography sx={{fontWeight:900,fontSize:18}}>{STEPS.find(x=>x[0]===step)?.[1]}</Typography>
    <Typography sx={{color:'#64748b',fontSize:13,mt:.5}}>
     This platform step is tracked centrally. The institute's operational configuration remains tenant-scoped and is completed through its corresponding module.
    </Typography>
    <Divider sx={{my:2}}/>
    <Stack direction="row" flexWrap="wrap" gap={1}>
     {Object.entries(counts).map(([key,value])=><Chip key={key} variant="outlined" label={key+' · '+value}/>)}
    </Stack>
    <Stack direction={{xs:'column',sm:'row'}} spacing={1} sx={{mt:2}}>
     <Button variant="contained" disabled={saving} onClick={saveAndComplete}>Mark step complete & continue</Button>
     {step==='REVIEW_GO_LIVE'&&<Button variant="contained" color="success" startIcon={<PlayArrowRounded/>} disabled={saving||completed.length<STEPS.length-1} onClick={goLive}>Go Live</Button>}
    </Stack>
   </CardContent>
  </Card>}
 </Stack>;
}
