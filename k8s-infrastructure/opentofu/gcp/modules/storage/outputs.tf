output "bucket_names" {
  value = { for key, bucket in google_storage_bucket.buckets : key => bucket.name }
}

output "bucket_urls" {
  value = { for key, bucket in google_storage_bucket.buckets : key => bucket.url }
}
