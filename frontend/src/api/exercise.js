import http from './http'

export const getExercises = (params) => http.get('/exercises', { params })
export const getExerciseMeta = () => http.get('/exercises/meta')
export const getExercise = (id) => http.get(`/exercises/${id}`)
