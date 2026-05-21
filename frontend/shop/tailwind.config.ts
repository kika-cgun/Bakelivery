import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}', '../packages/ui/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        display: ['Calistoga', 'serif'],
        body:    ['Raleway', 'sans-serif'],
        mono:    ['"JetBrains Mono"', 'monospace'],
      },
      colors: {
        primary: {
          DEFAULT: '#b45309',
          hover:   '#92400e',
          fg:      '#ffffff',
        },
        accent: {
          DEFAULT: '#2563eb',
          fg:      '#ffffff',
        },
        brand: {
          bg:     '#FDF6EC',
          muted:  '#FFFCF8',
          border: '#EDD9B8',
        },
      },
      boxShadow: {
        primary: '0 4px 14px rgba(180,83,9,0.35)',
        card:    '0 1px 3px rgba(0,0,0,.04), 0 3px 0 rgba(180,83,9,.11)',
        lifted:  '0 1px 3px rgba(0,0,0,.04), 0 5px 0 rgba(180,83,9,.22)',
        login:   '0 2px 0 0 rgba(180,83,9,0.15), 0 4px 20px rgba(0,0,0,0.06)',
      },
      borderRadius: {
        '4xl': '2rem',
      },
    },
  },
  plugins: [],
} satisfies Config;
