import {useEffect,useMemo,useState} from 'react';
import {
 Alert,Box,Button,Card,CardContent,Chip,Divider,LinearProgress,MenuItem,
 Stack,Step,StepLabel,Stepper,TextField,Typography
} from '@mui/material';
import {CheckCircleRounded,ArrowForwardRounded,SaveRounded,PlayArrowRounded} from '@mui/icons-material';
import {api} from '../services';

const steps=[
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

const links:Record<string,string>={
 ACADEMIC_STRUCTURE:'/owner/academics',
 PROGRAMS_EXAMS:'/owner/academics',
 SUBJECTS:'/owner/academics',
 BATCHES:'/owner/academics',
 FACULTY:'/owner/faculty',
 FEE_PLANS:'/owner/finance',
 DATA_IMPORT:'/owner/students'
};

const labels:Record<string,string>={
 academicYears:'Academic years',classes:'Classes',streams:'Streams',subjects:'Subjects',
 programs:'Programs',batches:'Batches',teachers:'Faculty',classrooms:'Classrooms',students:'Students'
};

export default function InstituteSetupPage(){
 const[data,setData]=useState<any>(null),[form,setForm]=useState<any>({}),[active,setActive]=useState(0);
 const[done,setDone]=useState<string[]>([]),[error,setError]=useState(''),[saved,setSaved]=useState(false);
 const[busy,setBusy]=useState(false);

 async function load(){
  try{
   setError('');
   const r=await api.get('/institute-setup');
   const d=r?.data?.data??r?.data??{};
   setData(d);
   const p=d.profile||{};
   setForm({
    instituteName:p.institute_name||'',slug:p.slug||'',instituteType:p.institute_type||'',
    logoUrl:p.logo_url||'',primaryColor:p.primary_color||'',secondaryColor:p.secondary_color||'',
    timezone:p.timezone||'Asia/Kolkata',contactEmail:p.contact_email||'',
    contactPhone:p.contact_phone||'',address:p.address||'',notes:p.notes||''
   });
   let raw=p.completed_steps;
   if(typeof raw==='string'){try{raw=JSON.parse(raw)}catch{raw=[]}}
   setDone(Array.isArray(raw)?raw:[]);
   const idx=steps.findIndex(s=>s[0]===p.current_step);
   setActive(idx<0?0:idx);
  }catch(e:any){setError(e?.response?.data?.message||'Unable to load institute setup.')}
 }
 useEffect(()=>{load()},[]);

 const progress=useMemo(()=>Math.round((done.length/steps.length)*100),[done]);
 const counts=data?.counts||{};
 const current=steps[active]?.[0]||'INSTITUTE_PROFILE';

 async function saveProfile(){
  try{setBusy(true);setError('');await api.put('/institute-setup/profile',form);await saveProgress(current);setSaved(true);setTimeout(()=>setSaved(false),2200);await load()}
  catch(e:any){setError(e?.response?.data?.message||'Unable to save setup.')}finally{setBusy(false)}
 }

 async function saveProgress(step=current,status?:string){
  const next=done.includes(step)?done:[...done,step];
  await api.put('/institute-setup/progress',{
   currentStep:step,status:status||'CONFIGURING',completedSteps:JSON.stringify(next)
  });
  setDone(next);
 }

 async function next(){
  try{
   setBusy(true);setError('');
   if(active===0){
    const name=String(form.instituteName||'').trim();
    const slug=String(form.slug||'').trim();
    if(!name||!slug) throw new Error('Institute name and slug are required.');
    await api.put('/institute-setup/profile',form);
   }
   const nextIndex=Math.min(active+1,steps.length-1);
   const step=steps[nextIndex][0];
   await saveProgress(current,undefined);
   setActive(nextIndex);
   await api.put('/institute-setup/progress',{currentStep:step,status:'CONFIGURING',completedSteps:JSON.stringify(done.includes(current)?done:[...done,current])});
  }catch(e:any){setError(e?.response?.data?.message||'Unable to save setup progress.')}finally{setBusy(false)}
 }

 async function ready(){
  try{
   setBusy(true);const all=steps.map(s=>s[0]);
   await api.put('/institute-setup/progress',{currentStep:'REVIEW_GO_LIVE',status:'READY_FOR_REVIEW',completedSteps:JSON.stringify(all)});
   setDone(all);setActive(8);await load();
  }catch(e:any){setError(e?.response?.data?.message||'Unable to mark setup ready.')}finally{setBusy(false)}
 }

 async function goLive(){
  try{
   setBusy(true);
   await api.put('/institute-setup/progress',{currentStep:'REVIEW_GO_LIVE',status:'GO_LIVE',completedSteps:JSON.stringify(steps.map(s=>s[0]))});
   await load();
  }catch(e:any){setError(e?.response?.data?.message||'Unable to complete go-live.')}finally{setBusy(false)}
 }

 if(!data) return <LinearProgress/>;

 const status=data.profile?.setup_status||'DRAFT';

 return <Stack spacing={2.5}>
  {error&&<Alert severity="error">{error}</Alert>}
  {saved&&<Alert severity="success">Institute setup saved.</Alert>}

  <Card variant="outlined" sx={{borderRadius:3}}>
   <CardContent sx={{p:{xs:2,md:2.75}}}>
    <Stack direction={{xs:'column',md:'row'}} spacing={2} justifyContent="space-between" alignItems={{md:'center'}}>
     <Box>
      <Typography sx={{fontSize:21,fontWeight:950}}>Institute Setup Center</Typography>
      <Typography sx={{fontSize:12.5,color:'#64748b',mt:.5}}>Configure the institute once. The Coaching OS adapts to its real academic structure.</Typography>
     </Box>
     <Chip label={status.replaceAll('_',' ')} color={status==='GO_LIVE'?'success':status==='READY_FOR_REVIEW'?'warning':'default'} sx={{fontWeight:850}}/>
    </Stack>
    <Box sx={{mt:2.25}}>
     <Stack direction="row" justifyContent="space-between" sx={{mb:.7}}>
      <Typography sx={{fontSize:11.5,fontWeight:800,color:'#475569'}}>Onboarding progress</Typography>
      <Typography sx={{fontSize:11.5,fontWeight:900}}>{progress}%</Typography>
     </Stack>
     <LinearProgress variant="determinate" value={progress} sx={{height:7,borderRadius:10}}/>
    </Box>
   </CardContent>
  </Card>

  <Card variant="outlined" sx={{borderRadius:3}}>
   <CardContent sx={{p:{xs:1.5,md:2.5},overflowX:'auto'}}>
    <Stepper activeStep={active} alternativeLabel sx={{minWidth:850}}>
     {steps.map(([key,label])=><Step key={key} completed={done.includes(key)}>
      <StepLabel>{label}</StepLabel>
     </Step>)}
    </Stepper>
   </CardContent>
  </Card>

  {active===0&&<Card variant="outlined" sx={{borderRadius:3}}>
   <CardContent sx={{p:{xs:2,md:2.75}}}>
    <Typography sx={{fontSize:18,fontWeight:900,mb:2}}>Institute Profile</Typography>
    <Stack spacing={2}>
     <Stack direction={{xs:'column',md:'row'}} spacing={2}>
      <TextField fullWidth label="Institute name" value={form.instituteName||''} onChange={e=>setForm({...form,instituteName:e.target.value})}/>
      <TextField fullWidth label="Institute slug" value={form.slug||''} onChange={e=>setForm({...form,slug:e.target.value})}/>
     </Stack>
     <Stack direction={{xs:'column',md:'row'}} spacing={2}>
      <TextField select fullWidth label="Institute type" value={form.instituteType||''} onChange={e=>setForm({...form,instituteType:e.target.value})}>
       {['SCHOOL_COACHING','COMPETITIVE_COACHING','MULTI_EXAM','HYBRID','OTHER'].map(x=><MenuItem key={x} value={x}>{x.replaceAll('_',' ')}</MenuItem>)}
      </TextField>
      <TextField fullWidth label="Timezone" value={form.timezone||''} onChange={e=>setForm({...form,timezone:e.target.value})}/>
     </Stack>
     <Stack direction={{xs:'column',md:'row'}} spacing={2}>
      <TextField fullWidth label="Contact email" value={form.contactEmail||''} onChange={e=>setForm({...form,contactEmail:e.target.value})}/>
      <TextField fullWidth label="Contact phone" value={form.contactPhone||''} onChange={e=>setForm({...form,contactPhone:e.target.value})}/>
     </Stack>
     <TextField fullWidth multiline minRows={2} label="Address" value={form.address||''} onChange={e=>setForm({...form,address:e.target.value})}/>
     <Stack direction={{xs:'column',md:'row'}} spacing={2}>
      <TextField fullWidth label="Logo URL" value={form.logoUrl||''} onChange={e=>setForm({...form,logoUrl:e.target.value})}/>
      <TextField fullWidth label="Primary color" value={form.primaryColor||''} onChange={e=>setForm({...form,primaryColor:e.target.value})}/>
      <TextField fullWidth label="Secondary color" value={form.secondaryColor||''} onChange={e=>setForm({...form,secondaryColor:e.target.value})}/>
     </Stack>
     <TextField fullWidth multiline minRows={2} label="Implementation notes" value={form.notes||''} onChange={e=>setForm({...form,notes:e.target.value})}/>
     <Stack direction="row" justifyContent="flex-end" spacing={1.25}>
      <Button variant="outlined" startIcon={<SaveRounded/>} onClick={saveProfile} disabled={busy}>Save profile</Button>
      <Button variant="contained" endIcon={<ArrowForwardRounded/>} onClick={next} disabled={busy}>Save & continue</Button>
     </Stack>
    </Stack>
   </CardContent>
  </Card>}

  {active>0&&<Card variant="outlined" sx={{borderRadius:3}}>
   <CardContent sx={{p:{xs:2,md:2.75}}}>
    <Typography sx={{fontSize:18,fontWeight:900}}>{steps[active][1]}</Typography>
    <Typography sx={{fontSize:13,color:'#64748b',mt:.55}}>
      Use the live module below to configure this part of the institute. Setup progress is tracked centrally.
    </Typography>
    <Divider sx={{my:2}}/>
    <Stack direction={{xs:'column',md:'row'}} spacing={1.5} alignItems={{md:'center'}} justifyContent="space-between">
     <Box>
      <Typography sx={{fontWeight:850}}>Current configuration snapshot</Typography>
      <Typography sx={{fontSize:12,color:'#64748b',mt:.35}}>
       {Object.entries(counts).filter(([k])=>['academicYears','classes','streams','subjects','programs','batches','teachers','classrooms','students'].includes(k)).map(([k,v])=>`${labels[k]}: ${v}`).join('  •  ')}
      </Typography>
     </Box>
     {links[current]&&<Button variant="contained" href={links[current]} endIcon={<ArrowForwardRounded/>}>Open module</Button>}
    </Stack>
    {active===8&&<Stack direction={{xs:'column',sm:'row'}} spacing={1.25} sx={{mt:3}}>
      <Button variant="outlined" onClick={ready} disabled={busy||status==='GO_LIVE'}>Mark ready for review</Button>
      <Button variant="contained" color="success" startIcon={<PlayArrowRounded/>} onClick={goLive} disabled={busy||status!=='READY_FOR_REVIEW'||status==='GO_LIVE'}>Go Live</Button>
    </Stack>}
   </CardContent>
  </Card>}

  {active>0&&<Stack direction="row" justifyContent="space-between">
   <Button disabled={active===0||busy} onClick={()=>setActive(Math.max(0,active-1))}>Back</Button>
   <Button variant="contained" endIcon={<ArrowForwardRounded/>} disabled={busy||active===8} onClick={next}>Save & continue</Button>
  </Stack>}
 </Stack>;
}
