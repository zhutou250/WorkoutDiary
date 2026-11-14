import http from './http'

export const listUsers = (params) => http.get('/admin/users', { params })
export const updateUserStatus = (userId, data) => http.put(`/admin/users/${userId}/status`, data)
export const getPlatform = () => http.get('/admin/platform')
export const listAdminExercises = (params) => http.get('/admin/exercises', { params })
export const createAdminExercise = (data) => http.post('/admin/exercises', data)
export const updateAdminExercise = (id, data) => http.put(`/admin/exercises/${id}`, data)
export const changeExerciseStatus = (id, status) =>
  http.put(`/admin/exercises/${id}/status`, null, { params: { status } })
export const listAuditLogs = (limit) => http.get('/admin/audit-logs', { params: { limit } })
