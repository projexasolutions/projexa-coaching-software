import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import App from './app/App';

const queryClient = new QueryClient();

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#0f766e', contrastText: '#ffffff' },
    background: { default: '#f6f8fb', paper: '#ffffff' },
    text: { primary: '#0f172a', secondary: '#64748b' },
    divider: '#e8edf3',
    success: { main: '#15803d' },
    warning: { main: '#b45309' },
    error: { main: '#dc2626' }
  },
  shape: { borderRadius: 12 },
  typography: {
    fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    button: { textTransform: 'none', fontWeight: 800, letterSpacing: 0 },
    h1: { fontWeight: 950 }
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 10, minHeight: 40, paddingInline: 16 }
      }
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          backgroundColor: '#fff',
          '& fieldset': { borderColor: '#dfe6ee' },
          '&:hover fieldset': { borderColor: '#b8c5d3' },
          '&.Mui-focused fieldset': { borderColor: '#0f766e', borderWidth: 1 }
        }
      }
    },
    MuiSelect: {
      styleOverrides: {
        root: { borderRadius: 10 }
      }
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { borderRadius: 14, border: '1px solid #e8edf3', boxShadow: '0 1px 2px rgba(15,23,42,.02)' }
      }
    },
    MuiTableCell: {
      styleOverrides: {
        head: { color: '#64748b', fontWeight: 850, fontSize: 11, textTransform: 'uppercase', letterSpacing: '.04em', backgroundColor: '#fafbfc' },
        root: { borderColor: '#edf1f5', paddingTop: 12, paddingBottom: 12 }
      }
    },
    MuiChip: {
      styleOverrides: { root: { fontWeight: 750, borderRadius: 8 } }
    },
    MuiDialog: {
      styleOverrides: {
        paper: { borderRadius: 16, boxShadow: '0 24px 70px rgba(15,23,42,.16)' }
      }
    }
  }
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <App />
      </ThemeProvider>
    </QueryClientProvider>
  </React.StrictMode>
);
