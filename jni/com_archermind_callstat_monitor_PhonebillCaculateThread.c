/* DO NOT EDIT THIS FILE - it is machine generated */
#include "com_archermind_callstat_monitor_PhonebillCaculateThread.h"
#include <stdlib.h>
#include <math.h>
#include <memory.h>
#include <android/log.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "calculate-jni", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "calculate-jni", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "calculate-jni", __VA_ARGS__))
#define PRECISION 1.0e-10

int Rank(double a[],int m,int n);//函数的声明(求矩阵秩的子函数)
int IsZeroVec(double* Vec,int VecDim);//判断一个向量是零向量的函数
void SwapRows(double* Matrix,int Row1,int Row2,int n);//交换矩阵的两行的函数
int brinv(double a[],int n);//求矩阵逆矩阵的函数
double Determinant(int n,double c[]);//求矩阵行列式的函数
void MatrixMul(double a[],double b[],int m,int n,int k,double c[]);//求两个矩阵相乘的函数

/*
 * Class:     com_archermind_callstat_monitor_PhonebillCaculateThread
 * Method:    Rank
 * Signature: ([DII)I
 */
JNIEXPORT jint JNICALL Java_com_archermind_callstat_monitor_PhonebillCaculateThread_Rank
  (JNIEnv * env, jobject thiz, jdoubleArray doubleArray, jint m, jint n)
  {
	 jdouble* pba = (*env)->GetDoubleArrayElements(env,doubleArray, 0 );
	 int length = sizeof(double)*m*n;
	 double* pbb = (double*)malloc(length);
	 int i=0;
	 for(i=0;i<m*n;i++)
	 {
		 pbb[i] = pba[i];
	 }
	 

	  int kk = Rank(pbb,m,n);
	  //LOGE("kk is %d",kk);
	  free(pbb);
	  pbb = NULL;
	  (*env)->ReleaseDoubleArrayElements(env,doubleArray,pba,0);
	  return(kk);
  }

  int  Rank(double a[],int m,int n)   
  {    
		int order=0;//to be returned
		int RowIndex=0,ColIndex=0;
		int RowCnt,ColCnt;
		int SwapRow;
		double factor;
		int flag = 0;
        int j =0;
		while((RowIndex<m)&&(ColIndex<n))
		{
			while(ColIndex<n)
			{
				
				//intialize temp
				double temp[m-RowIndex];
				for(ColCnt=0;ColCnt<m-RowIndex;ColCnt++)
					temp[ColCnt]=a[(RowIndex+ColCnt)*n+ColIndex];
				flag=IsZeroVec(temp,m-RowIndex);
				
				if(flag)			
					ColIndex++;
				else
					break;
			}

			if(flag)
				break;
			else
			{
				if(fabs(a[RowIndex*n+ColIndex])<PRECISION)
				{
					SwapRow=RowIndex;
					for(RowCnt=RowIndex;RowCnt<m;RowCnt++)					
						if(fabs(a[RowCnt*n+ColIndex])>PRECISION)
						{
							SwapRow=RowCnt;
							break;
						}
					SwapRows(a,RowIndex,SwapRow,n);
				}

				for(RowCnt=RowIndex+1;RowCnt<m;RowCnt++)
				{
					factor=a[RowCnt*n+ColIndex]/a[RowIndex*n+ColIndex];
					for(ColCnt=ColIndex;ColCnt<n;ColCnt++)
						a[RowCnt*n+ColCnt]-=(factor*a[RowIndex*n+ColCnt]);
				}
			}
			RowIndex++;
			ColIndex++;
		}
		double ZeroTestVec[n];
		LOGE("高斯列主消元之后的结果:");
		for(RowCnt=0;RowCnt<m;RowCnt++){
			for(j=0;j<n;j++){
				ZeroTestVec[j] = a[RowCnt*n+j];
			}
		    LOGE("%f	%f	%f	%f	%f	%f",ZeroTestVec[0],ZeroTestVec[1],ZeroTestVec[2],ZeroTestVec[3],ZeroTestVec[4],ZeroTestVec[5]);
			if(!IsZeroVec(ZeroTestVec,n))
				order++;
		}
		return order; 
  } 
  
   int IsZeroVec(double* Vec,int VecDim){
		int flag=1;
		int Cnt;
		for(Cnt=0;Cnt<VecDim;Cnt++)
		{
			if(fabs(Vec[Cnt])>PRECISION)
			{
				flag=0;
				break;
			}
		}
		return flag;
	}
	
	void SwapRows(double* Matrix,int Row1,int Row2,int n){
		double temp[n];
		int Cnt;
		for(Cnt=0;Cnt<n;Cnt++){
			temp[Cnt]=Matrix[Row1*n+Cnt];
			Matrix[Row1*n+Cnt]=Matrix[Row2*n+Cnt];
			Matrix[Row2*n+Cnt]=temp[Cnt];
		}
		
	}  
	
/*
 * Class:     com_archermind_callstat_monitor_PhonebillCaculateThread
 * Method:    Transform
 * Signature: ([DII)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_com_archermind_callstat_monitor_PhonebillCaculateThread_Transform
  (JNIEnv * env, jobject thiz, jdoubleArray doubleArray, jint m, jint n)
  {
	  jdouble* pba = (*env)->GetDoubleArrayElements(env,doubleArray, 0 );
	  int length = sizeof(jdouble)*n*m;
	  jdouble* pbb = (jdouble*)malloc(length);
	  memset(pbb,0,length);
	  int i=0,j=0;
	  
	  for(i=0;i<m;i++)
	  {
		  for(j=0;j<n;j++)
		  {
			 pbb[m*j+i] = pba[n*i+j];
		  }
	  }
	  
	  jdoubleArray jretDoubleArray = NULL;
	  jretDoubleArray = (*env)->NewDoubleArray(env,m*n);
	  (*env)->SetDoubleArrayRegion(env,jretDoubleArray, 0, m*n, pbb);
	  (*env)->ReleaseDoubleArrayElements(env,doubleArray,pba,0);
	  free(pbb);
	  return jretDoubleArray;
  }

/*
 * Class:     com_archermind_callstat_monitor_PhonebillCaculateThread
 * Method:    Inverse
 * Signature: ([DI)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_com_archermind_callstat_monitor_PhonebillCaculateThread_Inverse
  (JNIEnv * env, jobject thiz, jdoubleArray doubleArray, jint m)
  {
	  jdouble* pba = (*env)->GetDoubleArrayElements(env,doubleArray, 0 );
      jdoubleArray jretDoubleArray = NULL;
	  jretDoubleArray = (*env)->NewDoubleArray(env,m*m);
	  
      brinv(pba,m);
	  (*env)->SetDoubleArrayRegion(env,jretDoubleArray, 0, m*m, pba);
	  (*env)->ReleaseDoubleArrayElements(env,doubleArray,pba,0);
	  return jretDoubleArray;
  }
  
  int brinv(double a[],int n) /*求矩阵的逆矩阵*/ 
                             //int n; /*矩阵的阶数*/ 
                             //double a[]; /*矩阵A*/ 
{ 
  int *is,*js,i,j,k,l,u,v; 
  double d,p; 
  is=malloc(n*sizeof(int)); 
  js=malloc(n*sizeof(int)); 
  for (k=0; k<=n-1; k++) 
  { 
   d=0.0; 
   for (i=k; i<=n-1; i++) 
   /*全选主元，即选取绝对值最大的元素*/ 
   for (j=k; j<=n-1; j++) 
   { 
    l=i*n+j; p=fabs(a[l]); 
    if (p>d) { d=p; is[k]=i; js[k]=j;} 
   } 
   /*全部为0，此时为奇异矩阵*/ 
   if (d+1.0==1.0) 
   { 
    free(is); free(js);
    return(0); 
   } 
   /*行交换*/ 
   if (is[k]!=k) 
   for (j=0; j<=n-1; j++) 
   { 
		u=k*n+j; v=is[k]*n+j; 
		p=a[u]; a[u]=a[v]; a[v]=p; 
   } 
   /*列交换*/ 
   if (js[k]!=k) 
   for (i=0; i<=n-1; i++) 
   { 
		u=i*n+k; v=i*n+js[k]; 
		p=a[u]; a[u]=a[v]; a[v]=p; 
   } 
   l=k*n+k; 
   a[l]=1.0/a[l]; /*求主元的倒数*/ 
   /* a[kj]a[kk] -> a[kj] */ 
   for (j=0; j<=n-1; j++) 
   if (j!=k) 
   { 
		u=k*n+j; a[u]=a[u]*a[l]; 
   } 
   /* a[ij] - a[ik]a[kj] -> a[ij] */ 
   for (i=0; i<=n-1; i++) 
   if (i!=k) 
   for (j=0; j<=n-1; j++) 
    if (j!=k) 
    { 
		u=i*n+j; 
		a[u]=a[u]-a[i*n+k]*a[k*n+j]; 
    } 
   /* -a[ik]a[kk] -> a[ik] */ 
   for (i=0; i<=n-1; i++) 
   if (i!=k) 
   { 
      u=i*n+k; a[u]=-a[u]*a[l]; 
   } 
  } 
  
  for (k=n-1; k>=0; k--) 
  { 
    /*恢复列*/ 
    if (js[k]!=k) 
    for (j=0; j<=n-1; j++) 
    { 
      u=k*n+j; v=js[k]*n+j; 
      p=a[u]; a[u]=a[v]; a[v]=p; 
    } 
    /*恢复行*/ 
    if (is[k]!=k) 
    for (i=0; i<=n-1; i++) 
    { 
		u=i*n+k; v=i*n+is[k]; 
		p=a[u]; a[u]=a[v]; a[v]=p; 
	} 
  } 
  free(is); free(js); 
  return(1); 
} 

double Determinant(int n,double c[])
{ 
  double a[10][10],b[9][9],d[100],s=0,e,w;int i,j,p,k=-1;
  if(n==1) return(c[0]);
  else if(n==2)
  {
	  e=c[0]*c[3]-c[1]*c[2];
	  return(e);
  }
  else if(n>2)
  {
	 for(i=0;i<n;i++)
       for(j=0;j<n;j++)
        a[i][j]=c[i*n+j];
   
    for(p=0;p<n;p++)
    {
		w=a[0][p];
        for(i=0;i<n-1;i++)
         for(j=0;j<p;j++)
           b[i][j]=a[i+1][j];
        for(i=0;i<n-1;i++)
         for(j=p;j<n-1;j++)
           b[i][j]=a[i+1][j+1];
        for(i=0;i<n-1;i++)
         for(j=0;j<n-1;j++)
           d[i*(n-1)+j]=b[i][j];
        k=k*(-1);
        s=s+k*w*Determinant(n-1,d);
    }
   return(s);
   } 
 }
 
/*
 * Class:     com_archermind_callstat_monitor_PhonebillCaculateThread
 * Method:    MultiMatrix
 * Signature: ([DI[DII)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_com_archermind_callstat_monitor_PhonebillCaculateThread_MultiMatrix
  (JNIEnv * env , jobject thiz, jdoubleArray doubleArrayA, jint m, jdoubleArray doubleArrayB, jint n, jint k)

  {
	  jdouble* pa = (*env)->GetDoubleArrayElements(env,doubleArrayA, 0 );
	  jdouble* pb = (*env)->GetDoubleArrayElements(env,doubleArrayB, 0 );
	  jdouble* pc = (jdouble*)malloc(sizeof(jdouble)*m*k);
	  MatrixMul(pa,pb,m,n,k,pc);
	  jdoubleArray jretDoubleArray = NULL;
	  jretDoubleArray = (*env)->NewDoubleArray(env,m*k);
	  (*env)->SetDoubleArrayRegion(env,jretDoubleArray, 0, m*k, pc);
	  (*env)->ReleaseDoubleArrayElements(env,doubleArrayA,pa,0);
	  (*env)->ReleaseDoubleArrayElements(env,doubleArrayB,pb,0);
	  return jretDoubleArray;
  }

void MatrixMul(a,b,m,n,k,c) /*实矩阵相乘*/ 
 int m,n,k; /*m:矩阵A的行数, n:矩阵B的行数, k:矩阵B的列数*/ 
 double a[],b[],c[]; /*a为A矩阵, b为B矩阵, c为结果，即c = AB */ 
 { 
	int i,j,l,u; 
	/*逐行逐列计算乘积*/ 
	for (i=0; i<=m-1; i++) 
	for (j=0; j<=k-1; j++) 
	{ 
	 u=i*k+j; c[u]=0.0; 
	 for (l=0; l<=n-1; l++) 
		c[u]=c[u]+a[i*n+l]*b[l*k+j]; 
	} 
	return; 
 } 
 

