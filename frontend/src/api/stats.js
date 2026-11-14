import http from './http'

export const getOverview = (weeks) => http.get('/stats/overview', { params: { weeks } })
export const getExerciseTrend = (exerciseId) => http.get(`/stats/exercises/${exerciseId}/trend`)
export const getWeightTrend = () => http.get('/stats/weight')
export const getCalendar = (year, month) => http.get('/stats/calendar', { params: { year, month } })
