import { HttpClient } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth/auth.service';
import { CandidateProfile } from '../candidate/candidate-profile.model';
import { JobPosting } from './job-posting.model';

type ApplyState = 'guest' | 'wrong-role' | 'missing-cv' | 'ready';

@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [RouterLink, MatButtonModule],
  templateUrl: './job-detail.component.html',
})
export class JobDetailComponent implements OnInit {
  private http = inject(HttpClient);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);

  readonly job = signal<JobPosting | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly hasCv = signal(false);
  readonly applying = signal(false);
  readonly applied = signal(false);
  readonly applyError = signal<string | null>(null);

  readonly applyState = computed<ApplyState>(() => {
    const user = this.auth.currentUser();
    if (!user) return 'guest';
    if (user.role !== 'CANDIDATE') return 'wrong-role';
    return this.hasCv() ? 'ready' : 'missing-cv';
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.http.get<JobPosting>(`/api/v1/jobs/${id}`).subscribe({
      next: (job) => {
        this.job.set(job);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.notFound.set(true);
      },
    });

    if (this.auth.hasRole('CANDIDATE')) {
      this.http.get<CandidateProfile>('/api/v1/candidates/me').subscribe((profile) => this.hasCv.set(profile.hasCv));
    }
  }

  apply(): void {
    const job = this.job();
    if (!job) return;
    this.applying.set(true);
    this.applyError.set(null);

    this.http.post(`/api/v1/jobs/${job.id}/apply`, {}).subscribe({
      next: () => {
        this.applying.set(false);
        this.applied.set(true);
      },
      error: (err) => {
        this.applying.set(false);
        this.applyError.set(
          err.status === 409 ? 'You have already applied to this job.' : 'Could not submit your application. Please try again.',
        );
      },
    });
  }
}
