{{- define "agentforge.labels" -}}
app.kubernetes.io/name: agentforge
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
{{- end }}

{{- define "agentforge.secretName" -}}
{{- default (printf "%s-secrets" .Release.Name) .Values.secret.existingSecret -}}
{{- end }}

