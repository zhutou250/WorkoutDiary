import http from './http'

export const listSessions = (params) => http.get('/sessions', { params })
export const getActiveSession = () => http.get('/sessions/active')
export const startSession = (planDayId) => http.post('/sessions/start', { planDayId })
export const startFreeSession = () => http.post('/sessions/free')
export const getSession = (id) => http.get(`/sessions/${id}`)
export const finishSession = (id, data) => http.post(`/sessions/${id}/finish`, data)
export const cancelSession = (id) => http.post(`/sessions/${id}/cancel`)

export const addSessionExercise = (sessionId, data) =>
  http.post(`/sessions/${sessionId}/exercises`, data)
export const replaceSessionExercise = (sessionId, sessionExerciseId, data) =>
  http.post(`/sessions/${sessionId}/exercises/${sessionExerciseId}/replace`, data)
export const skipSessionExercise = (sessionId, sessionExerciseId) =>
  http.post(`/sessions/${sessionId}/exercises/${sessionExerciseId}/skip`)

export const addWorkoutSet = (sessionId, sessionExerciseId, data) =>
  http.post(`/sessions/${sessionId}/exercises/${sessionExerciseId}/sets`, data)
export const updateWorkoutSet = (setId, data) => http.put(`/sessions/sets/${setId}`, data)
export const deleteWorkoutSet = (setId) => http.delete(`/sessions/sets/${setId}`)
