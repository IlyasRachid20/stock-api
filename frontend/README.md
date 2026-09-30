# Stock API dashboard

React + TypeScript web interface for the [Stock API](../README.md).

```bash
npm install
npm run dev        # http://localhost:5173, forwards /api to http://localhost:8080
```

Use another API address with `API_URL=http://localhost:8081 npm run dev`.

| Command | What it does |
|---|---|
| `npm run lint` | oxlint |
| `npm run typecheck` | TypeScript |
| `npm test` | Vitest + Testing Library (the app runs against a fake API) |
| `npm run build` | production build in `dist/` |

## Structure

```
src/
├── api/          fetch wrapper (JWT, error messages) and API types
├── auth/         session, login/logout, route guard
├── components/   shared pieces (loading and error states)
├── layout/       app shell: header, navigation
├── pages/        one file per page, each loaded on first use
├── utils/        formatting (money, dates)
└── test/         test setup and helpers
```
