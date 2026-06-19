package sample

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, AwsCredentials, AwsCredentialsProvider}

class CustomCredentialProvider private(private val credentials: AwsCredentials)
  extends AwsCredentialsProvider {

  override def resolveCredentials: AwsCredentials = this.credentials
}

object CustomCredentialProvider {

  def create(keys: java.util.Map[String, String]): CustomCredentialProvider = {
    val accessKeyId = keys.getOrDefault("accessKeyId", Constants.s3AccessKey)
    val secretAccessKey = keys.getOrDefault("secretAccessKey", Constants.s3SecretKey)
    new CustomCredentialProvider(AwsBasicCredentials.create(accessKeyId, secretAccessKey))
  }
}
