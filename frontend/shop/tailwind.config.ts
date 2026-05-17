import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}', '../packages/ui/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        display: ['Calistoga', 'serif'],
        body:    ['Inter', 'sans-serif'],
        mono:    ['"JetBrains Mono"', 'monospace'],
      },
      colors: {
        primary: {
          DEFAULT: '#d97706',
          hover:   '#b45309',
          fg:      '#ffffff',
        },
        accent: {
          DEFAULT: '#2563eb',
          fg:      '#ffffff',
        },
        brand: {
          bg:     '#FFF7ED',
          muted:  '#FDF4F0',
          border: '#FCEAE1',
        },
      },
      boxShadow: {
        primary: '0 4px 14px rgba(217,119,6,0.30)',
      },
      borderRadius: {
        '4xl': '2rem',
      },
    },
  },
  plugins: [],
} satisfies Config;
