export interface JobPosting {
  id: string;
  title: string;
  description: string;
  sector: string;
  city: string;
  contractType: string;
  status: string;
  companyName: string | null;
}
