{{- define "platform-dashboard.name" -}}forwardmeasure-platform-dashboard{{- end -}}
{{- define "platform-dashboard.labels" -}}
app.kubernetes.io/name: {{ include "platform-dashboard.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}
{{- define "platform-dashboard.selectorLabels" -}}
app.kubernetes.io/name: {{ include "platform-dashboard.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
{{- define "platform-dashboard.image" -}}
{{- if .Values.image.digest -}}
{{- .Values.image.repository }}@{{ .Values.image.digest -}}
{{- else -}}
{{- if .Values.imagePolicy.requireDigest -}}
{{- fail (printf "Production image %s must be pinned by digest" .Values.image.repository) -}}
{{- end -}}
{{- .Values.image.repository }}:{{ required "The dashboard image requires a digest or explicit tag" .Values.image.tag -}}
{{- end -}}
{{- end -}}
