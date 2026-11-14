import http from './http'

export const listMetrics = (params) => http.get('/metrics', { params })
export const upsertMetric = (data) => http.post('/metrics', data)
export const deleteMetric = (id) => http.delete(`/metrics/${id}`)
