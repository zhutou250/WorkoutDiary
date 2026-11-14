import http from './http'

export const listPlans = () => http.get('/plans')
export const listPlanTemplates = () => http.get('/plans/templates')
export const copyPlanTemplate = (templateId) => http.post(`/plans/${templateId}/copy`)
export const createPlan = (data) => http.post('/plans', data)
export const getPlan = (id) => http.get(`/plans/${id}`)
export const updatePlan = (id, data) => http.put(`/plans/${id}`, data)
export const deletePlan = (id) => http.delete(`/plans/${id}`)

export const addPlanDay = (planId, data) => http.post(`/plans/${planId}/days`, data)
export const updatePlanDay = (dayId, data) => http.put(`/plans/days/${dayId}`, data)
export const deletePlanDay = (dayId) => http.delete(`/plans/days/${dayId}`)

export const addPlanExercise = (dayId, data) => http.post(`/plans/days/${dayId}/exercises`, data)
export const updatePlanExercise = (id, data) => http.put(`/plans/exercises/${id}`, data)
export const deletePlanExercise = (id) => http.delete(`/plans/exercises/${id}`)
