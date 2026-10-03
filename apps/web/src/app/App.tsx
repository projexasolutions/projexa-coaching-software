import {useState,type ReactNode} from 'react';
import {BrowserRouter,Routes,Route,Navigate,NavLink,useParams} from 'react-router-dom';
import {AppBar,Avatar,Box,Button,Chip,Divider,Drawer,IconButton,List,ListItemButton,ListItemIcon,ListItemText,Menu,MenuItem,Stack,Toolbar,Typography} from '@mui/material';
import {DashboardRounded,PeopleRounded,SchoolRounded,EventAvailableRounded,CalendarMonthRounded,AssignmentRounded,QuizRounded,AutoAwesomeRounded,AccountBalanceWalletRounded,CampaignRounded,NotificationsRounded,BarChartRounded,SupportAgentRounded,SettingsRounded,LogoutRounded,MenuRounded} from '@mui/icons-material';
import LoginPage from '../features/auth/LoginPage';
import {useAuth} from '../features/auth/authStore';
import {LiveDashboard,LiveStudentsPage,LiveStudentProfile,LiveAcademicsPage,LiveAttendancePage,LiveFinancePage} from '../features/Phase1Pages';
import TimetablePage from '../features/TimetablePage';
import FacultyPage from '../features/FacultyPage';
import HomeworkPage from '../features/HomeworkPage';
import ExamsPage from '../features/ExamsPage';
import ResultsPage from '../features/ResultsPage';
import IntelligencePage from '../features/IntelligencePage';

const nav:any[]=[
 ['/owner','Command Center',DashboardRounded],['/owner/students','Students',PeopleRounded],['/owner/academics','Academics',SchoolRounded],
 ['/owner/attendance','Attendance',EventAvailableRounded],['/owner/timetable','Timetable',CalendarMonthRounded],['/owner/faculty','Faculty',PeopleRounded],['/owner/homework','Homework',AssignmentRounded],['/owner/results','Results & Analytics',BarChartRounded],['/owner/intelligence','AI Intelligence',AutoAwesomeRounded],['/owner/exams','Exams & Results',QuizRounded],
 ['/owner/finance','Finance',AccountBalanceWalletRounded],['/owner/admissions','Admissions',CampaignRounded],['/owner/communication','Communication',NotificationsRounded],
 ['/owner/reports','Reports',BarChartRounded],['/owner/support','Support',SupportAgentRounded],['/owner/settings','Settings',SettingsRounded]
];

function Shell({children}:{children:ReactNode}){
 const session=useAuth(s=>s.session),logout=useAuth(s=>s.logout);const [mobile,setMobile]=useState(false);const [anchor,setAnchor]=useState<HTMLElement|null>(null);
 const side=<Box sx={{height:'100%',display:'flex',flexDirection:'column',bgcolor:'#fff'}}><Toolbar sx={{minHeight:'72px !important',px:2.5}}>
  <Box sx={{width:38,height:38,borderRadius:2,display:'grid',placeItems:'center',bgcolor:'#0f766e',color:'#fff',fontWeight:900}}>P</Box><Box sx={{ml:1.2}}><Typography sx={{fontWeight:900,letterSpacing:-.5}}>PROJEXA</Typography><Typography sx={{fontSize:10,color:'#94a3b8',fontWeight:800,letterSpacing:1.2}}>COACHING OS</Typography></Box>
 </Toolbar><Divider/><Box sx={{p:1.5,flex:1,overflowY:'auto'}}><Typography sx={{px:1.2,mb:1,fontSize:10,fontWeight:900,color:'#94a3b8',letterSpacing:1.2}}>WORKSPACE</Typography><List disablePadding>{nav.map(([to,label,Icon])=><ListItemButton key={to} component={NavLink} to={to} onClick={()=>setMobile(false)} sx={{mb:.4,borderRadius:2,py:1,color:'#64748b','&.active':{bgcolor:'#e8f8f5',color:'#0f766e'}}}><ListItemIcon sx={{minWidth:34,color:'inherit'}}><Icon fontSize="small"/></ListItemIcon><ListItemText primary={label} slotProps={{primary:{sx:{fontSize:13,fontWeight:700}}}}/></ListItemButton>)}</List></Box>
 <Box sx={{p:1.5}}><Box sx={{p:1.5,bgcolor:'#f8fafc',border:'1px solid #e5e7eb',borderRadius:2}}><Typography sx={{fontSize:12,fontWeight:900}}>Institute workspace</Typography><Typography sx={{fontSize:11,color:'#64748b',mt:.5}}>Tenant-isolated operational system.</Typography></Box></Box></Box>;
 return <Box sx={{display:'flex',minHeight:'100vh',bgcolor:'#f7f9fc'}}>{mobile?<Drawer open onClose={()=>setMobile(false)} slotProps={{paper:{sx:{width:272}}}}>{side}</Drawer>:<Drawer variant="permanent" slotProps={{paper:{sx:{width:260,borderRight:'1px solid #e5e7eb'}}}} sx={{width:260,flexShrink:0}}>{side}</Drawer>}<Box sx={{flex:1,minWidth:0}}><AppBar position="sticky" elevation={0} color="inherit" sx={{bgcolor:'rgba(255,255,255,.94)',borderBottom:'1px solid #e5e7eb'}}><Toolbar sx={{gap:1}}><IconButton onClick={()=>setMobile(true)} sx={{display:{md:'none'}}}><MenuRounded/></IconButton><Box sx={{flex:1}}><Typography sx={{fontWeight:800,fontSize:14}}>{session?.tenantName||'Institute'}</Typography><Typography sx={{fontSize:11,color:'#64748b'}}>Academic workspace</Typography></Box><IconButton onClick={e=>setAnchor(e.currentTarget)}><Avatar sx={{width:34,height:34,bgcolor:'#0f766e',fontSize:13}}>OP</Avatar></IconButton><Menu anchorEl={anchor} open={Boolean(anchor)} onClose={()=>setAnchor(null)}><MenuItem onClick={logout}><LogoutRounded fontSize="small"/><Box component="span" sx={{ml:1}}>Sign out</Box></MenuItem></Menu></Toolbar></AppBar>{children}</Box></Box>
}
function Page({title,subtitle,children}:{title:string;subtitle:string;children:ReactNode}){return <Box sx={{p:{xs:2,md:3,lg:4},maxWidth:1600,mx:'auto'}}><Box sx={{mb:3}}><Typography component="h1" sx={{fontSize:{xs:25,md:30},fontWeight:900,letterSpacing:-1}}>{title}</Typography><Typography sx={{fontSize:14,color:'#64748b',mt:.5}}>{subtitle}</Typography></Box>{children}</Box>}
function Guard({children}:{children:ReactNode}){return useAuth(s=>s.session)?children:<Navigate to="/login" replace/>}
function Profile(){const {id}=useParams();return <LiveStudentProfile id={id||''}/>}

function Placeholder({title}:{title:string}){return <Box sx={{p:5,textAlign:'center',bgcolor:'#fff',border:'1px solid #e5e7eb',borderRadius:3}}><Typography sx={{fontSize:22,fontWeight:900}}>{title}</Typography><Typography sx={{color:'#64748b',mt:1}}>This module is scheduled for the next implementation stage. Phase 1 operational modules are live.</Typography><Chip label="Phase 1 in progress" sx={{mt:2}}/></Box>}

export default function App(){return <BrowserRouter><Routes>
 <Route path="/login" element={<LoginPage/>}/>
 <Route path="/owner" element={<Guard><Shell><Page title="Command Center" subtitle="Live operational view of your institute."><LiveDashboard/></Page></Shell></Guard>}/>
 <Route path="/owner/students" element={<Guard><Shell><Page title="Students" subtitle="Manage student records and operational data."><LiveStudentsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/students/:id" element={<Guard><Shell><Page title="Student Profile" subtitle="Live student record and fee ledger."><Profile/></Page></Shell></Guard>}/>
 <Route path="/owner/academics" element={<Guard><Shell><Page title="Academics" subtitle="Configure academic years, classes, streams, subjects and batches."><LiveAcademicsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/attendance" element={<Guard><Shell><Page title="Attendance" subtitle="Create sessions and mark live attendance."><LiveAttendancePage/></Page></Shell></Guard>}/>
 <Route path="/owner/finance" element={<Guard><Shell><Page title="Finance" subtitle="Manage invoices, collections and outstanding fees."><LiveFinancePage/></Page></Shell></Guard>}/>
 <Route path="/owner/timetable" element={<Guard><Shell><Page title="Timetable Management" subtitle="Schedule batches, teachers and classrooms with conflict protection."><TimetablePage/></Page></Shell></Guard>}/>
 <Route path="/owner/faculty" element={<Guard><Shell><Page title="Faculty Management" subtitle="Manage teachers, teaching assignments and classroom resources."><FacultyPage/></Page></Shell></Guard>}/>
 <Route path="/owner/homework" element={<Guard><Shell><Page title="Teaching & Learning" subtitle="Create homework and track student submissions."><HomeworkPage/></Page></Shell></Guard>}/>
 <Route path="/owner/exams" element={<Guard><Shell><Page title="Exams & Results" subtitle="Build exams, configure subjects and manage question sets."><ExamsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/results" element={<Guard><Shell><Page title="Results & Analytics" subtitle="Generate, publish and analyze examination performance."><ResultsPage/></Page></Shell></Guard>}/>
 <Route path="/owner/intelligence" element={<Guard><Shell><Page title="AI Intelligence" subtitle="Prioritize institute risks and learning opportunities from operational data."><IntelligencePage/></Page></Shell></Guard>}/>
 {['admissions','communication','reports','support','settings'].map(k=><Route key={k} path={`/owner/${k}`} element={<Guard><Shell><Page title={k[0].toUpperCase()+k.slice(1)} subtitle="Operational module."><Placeholder title={k[0].toUpperCase()+k.slice(1)}/></Page></Shell></Guard>}/>)}
 <Route path="*" element={<Navigate to="/owner" replace/>}/>
 </Routes></BrowserRouter>}
