## Role Management

```sql
INSERT INTO public.user_roles (user_id, role_id)
VALUES (
    (SELECT id FROM public.users WHERE username = 'test'),
    1
);
```
