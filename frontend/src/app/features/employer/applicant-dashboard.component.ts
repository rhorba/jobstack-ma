import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { Applicant } from './applicant.model';
import { PageResponse } from '../../shared/page-response.model';

const PAGE_SIZE = 20;

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
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  private jobId = '';

  ngOnInit(): void {
    this.jobId = this.route.snapshot.paramMap.get('id') ?? '';
    this.load();
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.set(this.page() + 1);
      this.load();
    }
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.set(this.page() - 1);
      this.load();
    }
  }

  private load(): void {
    this.loading.set(true);
    const params = new HttpParams().set('page', this.page()).set('size', PAGE_SIZE);
    this.http
      .get<PageResponse<Applicant>>(`/api/v1/employers/me/jobs/${this.jobId}/applicants`, { params })
      .subscribe({
        next: (result) => {
          this.applicants.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
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
