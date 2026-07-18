import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { JobPosting } from '../job/job-posting.model';

@Component({
  selector: 'app-moderation-queue',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, FormsModule],
  templateUrl: './moderation-queue.component.html',
})
export class ModerationQueueComponent implements OnInit {
  private http = inject(HttpClient);

  readonly postings = signal<JobPosting[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rejectingId = signal<string | null>(null);
  readonly reasonText = signal('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.http.get<JobPosting[]>('/api/v1/admin/postings').subscribe({
      next: (postings) => {
        this.postings.set(postings);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Could not load the moderation queue.');
      },
    });
  }

  approve(posting: JobPosting): void {
    this.moderate(posting.id, 'APPROVE');
  }

  remove(posting: JobPosting): void {
    this.moderate(posting.id, 'REMOVE');
  }

  startReject(posting: JobPosting): void {
    this.rejectingId.set(posting.id);
    this.reasonText.set('');
  }

  cancelReject(): void {
    this.rejectingId.set(null);
  }

  confirmReject(posting: JobPosting): void {
    if (!this.reasonText().trim()) return;
    this.moderate(posting.id, 'REJECT', this.reasonText());
  }

  private moderate(id: string, action: 'APPROVE' | 'REJECT' | 'REMOVE', reason?: string): void {
    this.error.set(null);
    this.http.post(`/api/v1/admin/postings/${id}/moderate`, { action, reason }).subscribe({
      next: () => {
        this.rejectingId.set(null);
        this.postings.set(this.postings().filter((p) => p.id !== id));
      },
      error: () => this.error.set('Could not apply that moderation action. Please try again.'),
    });
  }
}
