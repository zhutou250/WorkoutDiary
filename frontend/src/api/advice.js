import http from './http'

export const listAdvice = () => http.get('/advice')
export const getAdvice = (id) => http.get(`/advice/${id}`)
export const generateAdvice = () => http.post('/advice/generate')
export const feedbackAdvice = (id, data) => http.post(`/advice/${id}/feedback`, data)
