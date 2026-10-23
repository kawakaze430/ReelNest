export type Creator = {
  id:string; displayName:string; legalName:string; contactEmail:string;
  portfolioUrl:string|null; statement:string; status:'PENDING'|'APPROVED'|'REJECTED'|'SUSPENDED';
  reviewNote:string|null;
};
export type Draft = {
  id:string;name:string;synopsis:string;type:'MOVIE'|'SERIES';genre:string;
  language:string;ageRating:string;runtimeMinutes:number|null;releaseYear:number|null;
  status:'DRAFT'|'IN_REVIEW'|'PUBLISHED'|'REJECTED'|'UNPUBLISHED';accessTier:'FREE'|'PREMIUM';
};
export type Rights = {
  id:string;titleId:string;rightsBasis:string;proofReference:string;agreementNotes:string;
  validFrom:string;validUntil:string;territories:string[];status:string;reviewNote:string|null;
};
export type UploadIntent = {
  id:string;titleId:string;filename:string;status:string;expectedBytes:number;
  partSizeBytes:number;partCount:number;expiresAt:string;
};
export type ReviewTitle = {
  id:string;name:string;status:string;access_tier:string;creator_name:string;rights_status:string|null;
  valid_from:string|null;valid_until:string|null;uploaded_files:number;ready_assets:number;
};

export type MediaJob = {
  id:string;processing_status:string;duration_seconds:number|null;
  job_state:string|null;attempts:number|null;max_attempts:number|null;
  last_error:string|null;updated_at:string|null;
  progress_stage?:string|null;progress_percent?:number|null;
  processing_details?:unknown;
};
