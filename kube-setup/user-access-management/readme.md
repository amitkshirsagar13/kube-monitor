### Add User:

```bash
cd user
# Create Private Key
openssl genrsa -out amit.key 2048

# Create Certificate Request
openssl req -new -key amit.key -out amit.csr -subj "/CN=amit/O=eng"

BASE64AMIT=`cat amit.csr |base64|tr -d '\'

# Create Request in Kubernetes

cat <<EOF > amit-signing-request.yaml                                                                         ░▒▓ 1 ✘  0.39   21.92G   100% 
apiVersion: certificates.k8s.io/v1
kind: CertificateSigningRequest
metadata:
  name: amit-csr
spec:
  signerName: kubernetes.io/kube-apiserver-client
  groups:
  - system:authenticated
  request: $BASE64AMIT
  usages:
  - digital signature
  - key encipherment
  - client auth
EOF

# Create Kubernetes Object for csr
kubectl apply -f amit-signing-request.yaml
kubectl get csr

# Approve certificate request as Admin
kubectl certificate approve amit-csr
kubectl get csr amit-csr -o yaml

# Extract Certificate to file
kubectl get csr amit-csr -o jsonpath='{.status.certificate}'| base64 -d > amit.crt

# Create Role with resource and verbs/actions
cat << EOF > amit-role.yaml
kind: Role
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  namespace: echo
  name: amit
rules:
- apiGroups: ["", "extensions", "apps"]
  resources: ["deployments", "replicasets", "pods"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"] # You can also use ["*"]
EOF

# Create Role binding with User
cat << EOF > amit-rolebinding.yaml
kind: RoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: amit-binding
  namespace: echo
subjects:
- kind: User
  name: amit
  apiGroup: ""
roleRef:
  kind: Role
  name: amit
  apiGroup: ""
EOF

# Check if role allowed for echo namespace
kubectl auth can-i list pods --namespace echo --as amit

# Should not be able to access default pods
kubectl auth can-i list pods --as amit




### Cluster level roles for allowing cluster level resource handling

cat << EOF > cluster-role.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  # "namespace" omitted since ClusterRoles are not namespaced
  name: cluster-role
rules:
- apiGroups: [""]
  resources: ["nodes", "events"]
  verbs: ["get", "watch", "list"]
EOF


cat << EOF > cluster-rolebinding.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: cluster-rolebinding
subjects:
- apiGroup: rbac.authorization.k8s.io
  kind: User
  name: amit # Name is case sensitive
- apiGroup: rbac.authorization.k8s.io
  kind: Group
  name: eng # Name is case sensitive This is not yet working need to know how to create group, we dont create group in kuberenetes, its property of users
roleRef:
  kind: ClusterRole
  name: cluster-role
  apiGroup: rbac.authorization.k8s.io
EOF

### ServiceAccount:

cat << EOF > service-account.yaml
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRoleBinding
metadata:
  name: tiller-clusterrolebinding
subjects:
- kind: ServiceAccount
  name: tiller
  namespace: kube-system
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: ""

EOF


kubectl get serviceaccounts  

kubectl -n kube-system describe secret deployment-controller-token-9xznw|grep 'token:'| tr -s ' '|cut -d ' ' -f 2
kubectl -n kube-system get secret deployment-controller-token-9xznw -o yaml|grep 'token:'| tr -s ' '|cut -d ' ' -f 3| base64 --decode|cut -d '%' -f 1

```"# kube-monitor" 
