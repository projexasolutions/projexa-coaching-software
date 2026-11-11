import {useEffect,useMemo,useState} from 'react';
import {Alert,Box,Button,Card,CardContent,Chip,Dialog,DialogActions,DialogContent,DialogTitle,Divider,Grid,MenuItem,Stack,TextField,Typography} from '@mui/material';
import {AddRounded,AutoAwesomeRounded,CalendarMonthRounded,CheckCircleRounded,DeleteOutlineRounded,EditRounded,Instagram,FacebookRounded,SendRounded} from '@mui/icons-material';
import {api} from '../services';

type PostStatus='DRAFT'|'APPROVED'|'SCHEDULED'|'PUBLISHED';
type SocialPost={id:string;title:string;category:string;platforms:string;format:string;caption:string;hashtags:string;scheduled_for?:string|null;status:PostStatus;created_at?:string};
const ideas=[
 ['5 study mistakes students should avoid','EDUCATIONAL','Carousel','Preparing for your next exam? Avoid these five common study mistakes and build a smarter revision routine.'],
 ['Physics concept of the day','EDUCATIONAL','Single image','A strong concept foundation makes problem-solving easier. Save this concept and revise it before your next practice session.'],
 ['Admissions are open','PROMOTIONAL','Poster','Take the next step towards your academic goals. Enquire with our team to learn about upcoming batches and admission details.'],
 ['A small reminder to stay consistent','ENGAGEMENT','Quote','Progress comes from consistent effort. What is one study goal you are working on this week?'],
 ['A 7-day revision strategy','EDUCATIONAL','Carousel','Plan your revision, practise questions, review mistakes and leave time for a final recap. Make every study session count.'],
 ['Meet our faculty','BRANDING','Photo post','Get to know the educators guiding our students. Add verified faculty details and an approved photograph before publishing.'],
 ['Explore our upcoming batches','PROMOTIONAL','Poster','Looking for structured preparation? Contact our institute to check course details, batch timings and availability.'],
 ['Quick knowledge check','ENGAGEMENT','Quiz','Time for a quick challenge! Share your answer in the comments and explain how you reached it.'],
 ['How to improve concentration','EDUCATIONAL','Carousel','Try focused study sessions, short breaks and a distraction-free study space. Small habits can make learning more effective.'],
 ['Celebrate student progress','BRANDING','Photo post','Celebrate effort and progress. Add only verified student achievements and obtain permission before sharing names or photographs.'],
 ['Five exam-day mistakes to avoid','EDUCATIONAL','Carousel','Read instructions carefully, manage your time and review your answers. Prepare calmly and trust your practice.'],
 ['Admission enquiry reminder','PROMOTIONAL','Poster','Admissions enquiries are welcome. Contact our institute for verified course information and upcoming batch details.'],
 ['Subject revision checklist','EDUCATIONAL','Carousel','Review key concepts, practise important questions and note the topics that need another round of revision.'],
 ['Ask us about admissions','ENGAGEMENT','Question post','Have questions about courses, batches or the admission process? Leave a question and our team will help.'],
 ['Your next learning milestone','PROMOTIONAL','Creative','Set your next learning goal and take the first step today. Connect with our team to explore the right programme for you.']
] as const;
const toLocalDateTime=(d:Date)=>{const pad=(n:number)=>String(n).padStart(2,'0');return d.getFullYear()+'-'+pad(d.getMonth()+1)+'-'+pad(d.getDate())+'T'+pad(d.getHours())+':'+pad(d.getMinutes())+':00'};
const statusColor=(s:string):any=>s==='PUBLISHED'?'success':s==='SCHEDULED'?'info':s==='APPROVED'?'warning':'default';
const blank={title:'',category:'EDUCATIONAL',platforms:'INSTAGRAM,FACEBOOK',format:'Carousel',caption:'',hashtags:'#StudyTips #Learning',scheduledFor:'',status:'DRAFT' as PostStatus};
const emptyBrand={instituteName:'',targetAudience:'Students preparing for competitive and academic exams',brandTone:'Friendly and professional',websiteUrl:'',callToAction:'Contact our institute for course details'};
export default function SocialStudioPage(){
 const [posts,setPosts]=useState<SocialPost[]>([]),[loading,setLoading]=useState(true),[error,setError]=useState(''),[busy,setBusy]=useState(false);
 const [open,setOpen]=useState(false),[editing,setEditing]=useState<string|null>(null),[form,setForm]=useState(blank);
 const [brandOpen,setBrandOpen]=useState(false),[brand,setBrand]=useState(emptyBrand),[brandSaved,setBrandSaved]=useState(false);
 const load=async()=>{setLoading(true);try{const r=await api.get('/social-studio/posts');const d=r.data?.data??r.data??[];setPosts(Array.isArray(d)?d:[]);setError('')}catch(e:any){setError(e?.response?.data?.message||'Unable to load Social Studio posts. Check that the API migration has been applied.')}finally{setLoading(false)}};
 useEffect(()=>{void load();void (async()=>{try{const r=await api.get('/social-studio/brand-profile');const d=r.data?.data??r.data??{};setBrand({instituteName:d.institute_name||'',targetAudience:d.target_audience||emptyBrand.targetAudience,brandTone:d.brand_tone||emptyBrand.brandTone,websiteUrl:d.website_url||'',callToAction:d.call_to_action||emptyBrand.callToAction})}catch{ /* profile can be configured when the module is available */ }})();},[]);
 const counts=useMemo(()=>({total:posts.length,drafts:posts.filter(p=>p.status==='DRAFT').length,approved:posts.filter(p=>p.status==='APPROVED').length,scheduled:posts.filter(p=>p.status==='SCHEDULED').length}),[posts]);
 function newPost(){setEditing(null);setForm(blank);setOpen(true)}
 function editPost(p:SocialPost){setEditing(p.id);setForm({title:p.title,category:p.category,platforms:p.platforms,format:p.format,caption:p.caption,hashtags:p.hashtags,scheduledFor:p.scheduled_for||'',status:p.status});setOpen(true)}
 async function generateCaption(){if(!form.title.trim()){setError('Add a post title before generating a caption');return}setBusy(true);try{const r=await api.post('/social-studio/ai/caption',{title:form.title,category:form.category,format:form.format,instituteName:brand.instituteName,targetAudience:brand.targetAudience,brandTone:brand.brandTone,callToAction:brand.callToAction});const d=r.data?.data??r.data??{};setForm(current=>({...current,caption:d.caption||current.caption,hashtags:d.hashtags||current.hashtags}));setError('')}catch(e:any){setError(e?.response?.data?.message||'AI caption generation is unavailable. Configure OPENAI_API_KEY on the API server.')}finally{setBusy(false)}}
 async function saveBrand(){setBusy(true);setBrandSaved(false);try{await api.put('/social-studio/brand-profile',{instituteName:brand.instituteName,targetAudience:brand.targetAudience,brandTone:brand.brandTone,websiteUrl:brand.websiteUrl,callToAction:brand.callToAction});setBrandSaved(true);setBrandOpen(false)}catch(e:any){setError(e?.response?.data?.message||'Unable to save brand profile')}finally{setBusy(false)}}
 async function save(){setBusy(true);try{const payload={...form,scheduledFor:form.scheduledFor||null};if(editing)await api.put('/social-studio/posts/'+editing,payload);else await api.post('/social-studio/posts',payload);setOpen(false);await load()}catch(e:any){setError(e?.response?.data?.message||'Unable to save post')}finally{setBusy(false)}}
 async function updateStatus(p:SocialPost,status:PostStatus){try{await api.put('/social-studio/posts/'+p.id,{title:p.title,category:p.category,platforms:p.platforms,format:p.format,caption:p.caption,hashtags:p.hashtags,scheduledFor:p.scheduled_for||null,status});await load()}catch(e:any){setError(e?.response?.data?.message||'Unable to update post status')}}
 async function remove(p:SocialPost){if(!window.confirm('Delete this draft?'))return;try{await api.delete('/social-studio/posts/'+p.id);await load()}catch(e:any){setError(e?.response?.data?.message||'Unable to delete post')}}
 async function generatePlan(){setBusy(true);try{const start=new Date();for(let i=0;i<ideas.length;i++){const d=new Date(start);d.setDate(d.getDate()+i*2);d.setHours(11,0,0,0);const [title,category,format,caption]=ideas[i];await api.post('/social-studio/posts',{title,category,platforms:'INSTAGRAM,FACEBOOK',format,caption,hashtags:'#Learning #Education #StudyTips',scheduledFor:toLocalDateTime(d),status:'DRAFT'});}await load()}catch(e:any){setError(e?.response?.data?.message||'Content plan generation stopped. Some posts may already have been saved.')}finally{setBusy(false)}}
 return <Stack spacing={2.5}>
  {error&&<Alert severity="error" onClose={()=>setError('')}>{error}</Alert>}
  <Card variant="outlined" sx={{borderRadius:3,borderColor:'#e6ebf2',background:'linear-gradient(110deg,#fff 0%,#f5f7ff 100%)'}}><CardContent sx={{p:{xs:2,md:3}}}>
   <Stack direction={{xs:'column',sm:'row'}} spacing={2} alignItems={{sm:'center'}} justifyContent="space-between">
    <Box><Chip icon={<AutoAwesomeRounded/>} label="SOCIAL STUDIO" size="small" color="primary" variant="outlined" sx={{mb:1}}/><Typography sx={{fontSize:{xs:22,md:26},fontWeight:900,color:'#14213d'}}>Plan a month of institute content</Typography><Typography sx={{fontSize:13,color:'#64748b',mt:.5}}>Create a 15-post starter calendar for Instagram and Facebook, then review every draft.</Typography></Box>
    <Stack direction={{xs:'column',sm:'row'}} spacing={1}><Button variant="outlined" onClick={()=>setBrandOpen(true)}>Brand profile</Button><Button variant="outlined" onClick={newPost} startIcon={<AddRounded/>}>Create post</Button><Button variant="contained" disabled={busy} onClick={generatePlan} startIcon={<AutoAwesomeRounded/>}>{busy?'Generating…':'Generate 15-post plan'}</Button></Stack>
   </Stack>
  </CardContent></Card>
  <Grid container spacing={1.5}>
   {[['Total posts',counts.total,'#3157b7'],['Drafts',counts.drafts,'#64748b'],['Approved',counts.approved,'#b7791f'],['Scheduled',counts.scheduled,'#16845b']].map(([label,value,color]:any)=><Grid item xs={6} md={3} key={label}><Card variant="outlined" sx={{borderRadius:3,borderColor:'#e8edf3'}}><CardContent><Typography sx={{fontSize:12,color:'#64748b'}}>{label}</Typography><Typography sx={{fontSize:28,fontWeight:900,color,mt:.5}}>{value}</Typography></CardContent></Card></Grid>)}
  </Grid>
  <Stack direction="row" alignItems="center" justifyContent="space-between"><Box><Typography sx={{fontSize:18,fontWeight:900,color:'#14213d'}}>Content calendar</Typography><Typography sx={{fontSize:12,color:'#64748b'}}>Review, edit and approve content before scheduling.</Typography></Box><Chip label={loading?'Loading':posts.length+' saved posts'} variant="outlined"/></Stack>
  {!loading&&!posts.length&&<Card variant="outlined" sx={{borderRadius:3}}><CardContent sx={{py:6,textAlign:'center'}}><CalendarMonthRounded sx={{fontSize:38,color:'#94a3b8'}}/><Typography sx={{fontWeight:800,mt:1}}>Your content calendar is empty</Typography><Typography sx={{fontSize:13,color:'#64748b',mt:.5}}>Generate a 15-post starter plan or create your first post.</Typography></CardContent></Card>}
  <Grid container spacing={1.5}>{posts.map(p=><Grid item xs={12} md={6} lg={4} key={p.id}><Card variant="outlined" sx={{height:'100%',borderRadius:3,borderColor:'#e8edf3'}}><CardContent><Stack spacing={1.2}>
   <Stack direction="row" justifyContent="space-between" alignItems="center"><Chip size="small" label={p.category.replace('_',' ')} variant="outlined"/><Chip size="small" label={p.status} color={statusColor(p.status)}/></Stack>
   <Typography sx={{fontWeight:850,fontSize:15,color:'#14213d'}}>{p.title}</Typography>
   <Typography sx={{fontSize:12,color:'#64748b'}}>{p.format} · {p.platforms?.replaceAll(',',' + ')}</Typography>
   <Typography sx={{fontSize:13,color:'#475569',display:'-webkit-box',WebkitLineClamp:3,WebkitBoxOrient:'vertical',overflow:'hidden'}}>{p.caption}</Typography>
   {p.scheduled_for&&<Typography sx={{fontSize:11,color:'#64748b'}}>Planned: {new Date(p.scheduled_for).toLocaleString()}</Typography>}
   <Divider/>
   <Stack direction="row" spacing={.5} alignItems="center"><Instagram sx={{fontSize:16,color:'#c13584'}}/><FacebookRounded sx={{fontSize:16,color:'#1877f2'}}/><Box sx={{flex:1}}/><Button size="small" onClick={()=>editPost(p)} startIcon={<EditRounded/>}>Edit</Button><Button size="small" color="error" onClick={()=>remove(p)}><DeleteOutlineRounded/></Button></Stack>
   {p.status==='DRAFT'?<Button size="small" variant="contained" onClick={()=>updateStatus(p,'APPROVED')} startIcon={<CheckCircleRounded/>}>Approve draft</Button>:p.status==='APPROVED'?<Button size="small" variant="outlined" disabled={!p.scheduled_for||new Date(p.scheduled_for).getTime()<=Date.now()} onClick={()=>updateStatus(p,'SCHEDULED')} startIcon={<CalendarMonthRounded/>}>Add to publishing queue</Button>:null}
  </Stack></CardContent></Card></Grid>)}</Grid>
  {brandSaved&&<Alert severity="success" onClose={()=>setBrandSaved(false)}>Institute brand profile saved. AI caption drafts will use this information.</Alert>}
  <Alert severity="info">This release stores content plans and approval status. AI caption generation requires OPENAI_API_KEY on the API server. Scheduling requires an approved post and a future date/time; posts are queued as planned content, not published automatically. Meta publishing and image generation are not active yet.</Alert>
  <Dialog open={brandOpen} onClose={()=>!busy&&setBrandOpen(false)} fullWidth maxWidth="sm"><DialogTitle>Institute brand profile</DialogTitle><DialogContent><Stack spacing={2} sx={{pt:1}}>
   <Typography sx={{fontSize:13,color:'#64748b'}}>Set verified institute details once. AI caption drafts will use this context to sound more consistent. Avoid adding private student information.</Typography>
   <TextField label="Institute name" value={brand.instituteName} onChange={e=>setBrand({...brand,instituteName:e.target.value})} required fullWidth/>
   <TextField label="Target audience" value={brand.targetAudience} onChange={e=>setBrand({...brand,targetAudience:e.target.value})} fullWidth/>
   <TextField select label="Brand tone" value={brand.brandTone} onChange={e=>setBrand({...brand,brandTone:e.target.value})} fullWidth>{['Friendly and professional','Energetic and motivating','Formal and academic','Simple and student-friendly','Premium and confident'].map(x=><MenuItem key={x} value={x}>{x}</MenuItem>)}</TextField>
   <TextField label="Website (optional)" value={brand.websiteUrl} onChange={e=>setBrand({...brand,websiteUrl:e.target.value})} fullWidth/>
   <TextField label="Preferred call to action" value={brand.callToAction} onChange={e=>setBrand({...brand,callToAction:e.target.value})} fullWidth/>
  </Stack></DialogContent><DialogActions sx={{p:2}}><Button onClick={()=>setBrandOpen(false)} disabled={busy}>Cancel</Button><Button variant="contained" onClick={saveBrand} disabled={busy||!brand.instituteName.trim()}>Save profile</Button></DialogActions></Dialog>
  <Dialog open={open} onClose={()=>!busy&&setOpen(false)} fullWidth maxWidth="sm"><DialogTitle>{editing?'Edit social post':'Create social post'}</DialogTitle><DialogContent><Stack spacing={2} sx={{pt:1}}>
   <TextField label="Post title" value={form.title} onChange={e=>setForm({...form,title:e.target.value})} required fullWidth/>
   <Stack direction={{xs:'column',sm:'row'}} spacing={1}><TextField select label="Category" value={form.category} onChange={e=>setForm({...form,category:e.target.value})} fullWidth>{['EDUCATIONAL','PROMOTIONAL','ENGAGEMENT','BRANDING'].map(x=><MenuItem key={x} value={x}>{x}</MenuItem>)}</TextField><TextField select label="Format" value={form.format} onChange={e=>setForm({...form,format:e.target.value})} fullWidth>{['Single image','Carousel','Poster','Photo post','Reel','Story','Quiz'].map(x=><MenuItem key={x} value={x}>{x}</MenuItem>)}</TextField></Stack>
   <TextField select label="Platforms" value={form.platforms} onChange={e=>setForm({...form,platforms:e.target.value})} fullWidth><MenuItem value="INSTAGRAM,FACEBOOK">Instagram + Facebook</MenuItem><MenuItem value="INSTAGRAM">Instagram</MenuItem><MenuItem value="FACEBOOK">Facebook</MenuItem></TextField>
   <Stack direction="row" justifyContent="space-between" alignItems="center"><Typography sx={{fontSize:13,fontWeight:800}}>Caption</Typography><Button size="small" onClick={generateCaption} disabled={busy||!form.title.trim()} startIcon={<AutoAwesomeRounded/>}>Draft with AI</Button></Stack><TextField label="Caption" value={form.caption} onChange={e=>setForm({...form,caption:e.target.value})} multiline minRows={4} fullWidth/>
   <TextField label="Hashtags" value={form.hashtags} onChange={e=>setForm({...form,hashtags:e.target.value})} fullWidth/>
   <TextField label="Planned date and time" type="datetime-local" value={form.scheduledFor} onChange={e=>setForm({...form,scheduledFor:e.target.value})} InputLabelProps={{shrink:true}} fullWidth/>
  </Stack></DialogContent><DialogActions sx={{p:2}}><Button onClick={()=>setOpen(false)} disabled={busy}>Cancel</Button><Button variant="contained" onClick={save} disabled={busy||!form.title.trim()||!form.caption.trim()}>Save draft</Button></DialogActions></Dialog>
 </Stack>;
}
