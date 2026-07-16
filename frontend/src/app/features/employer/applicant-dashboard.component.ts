import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { Applicant } from './applicant.model';

@Component({
  selector: 'app-applicant-dashboard',
  standalone: true,
  imports: [RouterLink, MatButtonModule],
  templateUrl: './applicant-dashboard.component.html',
})
export class ApplicantDashboardComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);

  readonly applicants = signal<Applicant[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  private jobId = '';

  ngOnInit(): void {
    this.jobId = this.route.snapshot.paramMap.get('id') ?? '';
    this.http.get<Applicant[]>(`/api/v1/employers/me/jobs/${this.jobId}/applicants`).subscribe({
      next: (applicants) => {
        this.applicants.set(applicants);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Could not load applicants for this job posting.');
      },
    });
  }

  downloadCv(applicant: Applicant): void {
    this.http.get(applicant.cvDownloadUrl, { responseType: 'blob' }).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${applicant.fullName ?? 'candidate'}-cv.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    });
  }
}
